package es.vargontoc.storyteller.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestClient;

class DirectorModelProvisionerTest {

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

        new DirectorModelProvisioner(apiWith(), client, "storyteller-director", definition).run(args);

        verify(request).body(Map.of("model", "storyteller-director", "from", "base", "system", "Hola\nmundo",
                "parameters", Map.of("temperature", 0.7, "num_ctx", 8192), "stream", false));
        verify(response).toBodilessEntity();
    }

    @Test
    void existingModelDoesNotCreateAgain() {
        RestClient client = mock(RestClient.class);

        new DirectorModelProvisioner(apiWith("storyteller-director:latest"), client, "storyteller-director", definition)
                .run(args);

        verify(client, never()).post();
    }

    @Test
    void modelfileIsResolvedFromClasspath() throws Exception {
        ClassPathResource resource = new ClassPathResource(DirectorModelProvisioner.MODELFILE_PATH);
        assertTrue(resource.exists());
        assertNotNull(resource.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("prompts/director.modelfile", DirectorModelProvisioner.MODELFILE_PATH);
    }

    @Test
    void unavailableOllamaFailsStartup() {
        OllamaApi api = mock(OllamaApi.class);
        when(api.listModels()).thenThrow(new IllegalStateException("offline"));

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new DirectorModelProvisioner(api, mock(RestClient.class), "storyteller-director", definition)
                        .run(args));
        assertEquals("offline", e.getCause().getMessage());
    }

    @Test
    void createFailureFailsStartup() {
        RestClient client = mock(RestClient.class);
        when(client.post()).thenThrow(new IllegalStateException("create failed"));

        assertThrows(IllegalStateException.class,
                () -> new DirectorModelProvisioner(apiWith(), client, "storyteller-director", definition).run(args));
    }

    @Test
    void httpErrorMessageIncludesStatusAndBody() {
        RestClient client = mock(RestClient.class);
        when(client.post()).thenThrow(org.springframework.web.client.HttpClientErrorException.create(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Bad Request", new org.springframework.http.HttpHeaders(),
                "{\"error\":\"invalid model\"}".getBytes(), null));

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new DirectorModelProvisioner(apiWith(), client, "storyteller-director", definition).run(args));
        assertTrue(e.getMessage().contains("400"));
        assertTrue(e.getMessage().contains("invalid model"));
    }

    @Test
    void modelfileWithoutFromFailsStartup() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new DirectorModelProvisioner(apiWith(), mock(RestClient.class), "storyteller-director",
                        new ByteArrayResource("SYSTEM x".getBytes())).run(args));
        assertTrue(e.getMessage().contains("FROM"));
    }

    @Test
    void realModelfileIsTranslated() throws Exception {
        RestClient client = mock(RestClient.class);
        RestClient.RequestBodyUriSpec request = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec body = mock(RestClient.RequestBodySpec.class);
        when(client.post()).thenReturn(request);
        when(request.uri("/api/create")).thenReturn(request);
        when(request.contentType(any())).thenReturn(request);
        when(request.body(any(Object.class))).thenReturn(body);
        when(body.retrieve()).thenReturn(mock(RestClient.ResponseSpec.class));

        new DirectorModelProvisioner(apiWith(), client, "storyteller-director",
                new ClassPathResource(DirectorModelProvisioner.MODELFILE_PATH)).run(args);

        org.mockito.ArgumentCaptor<Object> captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(request).body(captor.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> sent = (Map<String, Object>) captor.getValue();
        assertEquals("qwen2.5:14b-instruct", sent.get("from"));
        assertTrue(((String) sent.get("system")).startsWith("Eres el AGENTE DIRECTOR"));
        assertTrue(((String) sent.get("system")).endsWith("fuera del JSON."));
        assertEquals(8192, ((Map<?, ?>) sent.get("parameters")).get("num_ctx"));
    }
}
