package es.vargontoc.storyteller.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestClient;

class WriterModelProvisionerTest {

    private final ApplicationArguments args = mock(ApplicationArguments.class);
    private final ByteArrayResource definition = new ByteArrayResource(
            "FROM base\n\nPARAMETER temperature 0.7\nPARAMETER num_ctx 8192\n\nSYSTEM \"\"\"\nHola\nmundo\n\"\"\"\n".getBytes());

    private OllamaApi apiWith(String... models) {
        OllamaApi api = mock(OllamaApi.class);
        List<OllamaApi.Model> list = java.util.Arrays.stream(models).map(n -> {
            OllamaApi.Model m = mock(OllamaApi.Model.class);
            when(m.name()).thenReturn(n);
            return m;
        }).toList();
        when(api.listModels()).thenReturn(new OllamaApi.ListModelResponse(list));
        return api;
    }

    @Test
    void modelfileExistsInClasspathAndIsNotEmpty() throws Exception {
        ClassPathResource resource = new ClassPathResource(WriterModelProvisioner.MODELFILE_PATH);
        assertTrue(resource.exists());
        assertFalse(resource.getContentAsString(StandardCharsets.UTF_8).isBlank());
    }

    @Test
    void modelfileContainsOnomatopoeiaRules() throws Exception {
        String content = new ClassPathResource(WriterModelProvisioner.MODELFILE_PATH)
                .getContentAsString(StandardCharsets.UTF_8);
        assertTrue(content.contains("Como máximo una onomatopeya por página"));
        assertTrue(content.contains("Solo si el beat menciona un sonido natural en la escena"));
        assertTrue(content.contains("ocupa una de las cuatro frases"));
        assertTrue(content.contains("forma estándar y pronunciable del español"));
        assertTrue(content.contains("sin letras repetidas alargadas ni símbolos"));
        assertTrue(content.contains("Nunca incluyas onomatopeyas en la portada"));
    }

    @Test
    void modelfileUsesQwen3ForTheWriter() throws Exception {
        String content = new ClassPathResource(WriterModelProvisioner.MODELFILE_PATH)
                .getContentAsString(StandardCharsets.UTF_8);
        assertTrue(content.contains("FROM qwen3:14b"));
        assertFalse(content.contains("FROM qwen2.5"));
    }

    @Test
    void modelfileContainsLengthDialogueAndDramatizationRules() throws Exception {
        String content = new ClassPathResource(WriterModelProvisioner.MODELFILE_PATH)
                .getContentAsString(StandardCharsets.UTF_8);
        assertTrue(content.contains("4 frases y entre 45 y 60 palabras"));
        assertTrue(content.contains("Solo una frase puede ser corta"));
        assertTrue(content.contains("No escribas una frase por cada acción del beat"));
        assertTrue(content.contains("DIÁLOGO"));
        assertTrue(content.contains("al menos una línea de diálogo"));
        assertTrue(content.contains("MOSTRAR, NO CONTAR"));
        assertTrue(content.contains("EJEMPLO DE PÁGINA BIEN ESCRITA"));
        assertTrue(content.contains("EJEMPLO INCORRECTO"));
        assertTrue(content.contains("ANTES DE RESPONDER"));
        assertTrue(content.contains("No copies sus palabras"));
    }

    @Test
    void missingModelRegistersUpdatedModelfileWithOnomatopoeiaRules() {
        RestClient client = mock(RestClient.class);
        RestClient.RequestBodyUriSpec request = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec body = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec response = mock(RestClient.ResponseSpec.class);
        when(client.post()).thenReturn(request);
        when(request.uri("/api/create")).thenReturn(request);
        when(request.contentType(any())).thenReturn(request);
        when(request.body(any(Object.class))).thenReturn(body);
        when(body.retrieve()).thenReturn(response);
        org.mockito.ArgumentCaptor<Object> sent = org.mockito.ArgumentCaptor.forClass(Object.class);

        new WriterModelProvisioner(apiWith(), client, "storyteller-writer",
                new ClassPathResource(WriterModelProvisioner.MODELFILE_PATH)).run(args);

        verify(request).body(sent.capture());
        String system = (String) ((Map<?, ?>) sent.getValue()).get("system");
        assertTrue(system.contains("ONOMATOPEYAS"));
        assertTrue(system.contains("Nunca incluyas onomatopeyas en la portada"));
    }

    @Test
    void missingModelCreatesWithNameAndResourceContent() {
        RestClient client = mock(RestClient.class);
        RestClient.RequestBodyUriSpec request = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec body = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec response = mock(RestClient.ResponseSpec.class);
        when(client.post()).thenReturn(request);
        when(request.uri("/api/create")).thenReturn(request);
        when(request.contentType(any())).thenReturn(request);
        when(request.body(any(Object.class))).thenReturn(body);
        when(body.retrieve()).thenReturn(response);

        new WriterModelProvisioner(apiWith(), client, "storyteller-writer", definition).run(args);

        verify(request).body(Map.of("model", "storyteller-writer", "from", "base", "system", "Hola\nmundo",
                "parameters", Map.of("temperature", 0.7, "num_ctx", 8192), "stream", false));
        verify(response).toBodilessEntity();
    }

    @Test
    void existingModelDoesNotCreateAgain() {
        RestClient client = mock(RestClient.class);

        new WriterModelProvisioner(apiWith("storyteller-writer:latest"), client, "storyteller-writer", definition)
                .run(args);

        verify(client, never()).post();
    }

    @Test
    void httpErrorMessageIncludesStatusAndBody() {
        RestClient client = mock(RestClient.class);
        when(client.post()).thenThrow(org.springframework.web.client.HttpClientErrorException.create(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Bad Request", new org.springframework.http.HttpHeaders(),
                "{\"error\":\"invalid model\"}".getBytes(), null));

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new WriterModelProvisioner(apiWith(), client, "storyteller-writer", definition).run(args));
        assertTrue(e.getMessage().contains("400"));
        assertTrue(e.getMessage().contains("invalid model"));
    }

    @Test
    void modelfileWithoutFromFailsStartup() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new WriterModelProvisioner(apiWith(), mock(RestClient.class), "storyteller-writer",
                        new ByteArrayResource("SYSTEM x".getBytes())).run(args));
        assertTrue(e.getMessage().contains("FROM"));
    }
}
