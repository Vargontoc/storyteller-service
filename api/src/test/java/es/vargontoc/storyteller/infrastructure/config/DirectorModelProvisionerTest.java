package es.vargontoc.storyteller.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestClient;

class DirectorModelProvisionerTest {

    private final ApplicationArguments args = mock(ApplicationArguments.class);
    private final ByteArrayResource definition = new ByteArrayResource(
            "FROM base\n\nPARAMETER temperature 0.7\nPARAMETER num_ctx 8192\n\nSYSTEM \"\"\"\nHola\nmundo\n\"\"\"\n".getBytes());

    private RestClient.RequestBodyUriSpec stubCreate(RestClient client, RestClient.ResponseSpec response) {
        RestClient.RequestBodyUriSpec request = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec body = mock(RestClient.RequestBodySpec.class);
        when(client.post()).thenReturn(request);
        when(request.uri("/api/create")).thenReturn(request);
        when(request.contentType(any())).thenReturn(request);
        when(request.body(any(Object.class))).thenReturn(body);
        when(body.retrieve()).thenReturn(response);
        return request;
    }

    @Test
    void createsModelWithNameAndResourceContent() {
        RestClient client = mock(RestClient.class);
        RestClient.ResponseSpec response = mock(RestClient.ResponseSpec.class);
        RestClient.RequestBodyUriSpec request = stubCreate(client, response);

        new DirectorModelProvisioner(client, "storyteller-director", definition).run(args);

        verify(request).body(Map.of("model", "storyteller-director", "from", "base", "system", "Hola\nmundo",
                "parameters", Map.of("temperature", 0.7, "num_ctx", 8192), "stream", false));
        verify(response).toBodilessEntity();
    }

    @Test
    void runningAgainRegistersTheModelAgainSoItReplacesThePreviousOne() {
        RestClient client = mock(RestClient.class);
        RestClient.ResponseSpec response = mock(RestClient.ResponseSpec.class);
        stubCreate(client, response);
        DirectorModelProvisioner provisioner = new DirectorModelProvisioner(client, "storyteller-director", definition);

        provisioner.run(args);
        provisioner.run(args);

        verify(response, org.mockito.Mockito.times(2)).toBodilessEntity();
    }

    @Test
    void modelfileIsResolvedFromClasspath() throws Exception {
        ClassPathResource resource = new ClassPathResource(DirectorModelProvisioner.MODELFILE_PATH);
        assertTrue(resource.exists());
        assertNotNull(resource.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("prompts/director.modelfile", DirectorModelProvisioner.MODELFILE_PATH);
    }

    @Test
    void createFailureFailsStartup() {
        RestClient client = mock(RestClient.class);
        when(client.post()).thenThrow(new IllegalStateException("create failed"));

        assertThrows(IllegalStateException.class,
                () -> new DirectorModelProvisioner(client, "storyteller-director", definition).run(args));
    }

    @Test
    void httpErrorMessageIncludesStatusAndBody() {
        RestClient client = mock(RestClient.class);
        when(client.post()).thenThrow(org.springframework.web.client.HttpClientErrorException.create(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Bad Request", new org.springframework.http.HttpHeaders(),
                "{\"error\":\"invalid model\"}".getBytes(), null));

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new DirectorModelProvisioner(client, "storyteller-director", definition).run(args));
        assertTrue(e.getMessage().contains("400"));
        assertTrue(e.getMessage().contains("invalid model"));
    }

    @Test
    void modelfileWithoutFromFailsStartup() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new DirectorModelProvisioner(mock(RestClient.class), "storyteller-director",
                        new ByteArrayResource("SYSTEM x".getBytes())).run(args));
        assertTrue(e.getMessage().contains("FROM"));
    }

    @Test
    void realModelfileIsTranslatedWithSettingAndCoverRules() {
        RestClient client = mock(RestClient.class);
        RestClient.RequestBodyUriSpec request = stubCreate(client, mock(RestClient.ResponseSpec.class));

        new DirectorModelProvisioner(client, "storyteller-director",
                new ClassPathResource(DirectorModelProvisioner.MODELFILE_PATH)).run(args);

        org.mockito.ArgumentCaptor<Object> captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(request).body(captor.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> sent = (Map<String, Object>) captor.getValue();
        String system = (String) sent.get("system");
        assertEquals("qwen3:14b", sent.get("from"));
        assertTrue(system.startsWith("Eres el AGENTE DIRECTOR"));
        assertTrue(system.endsWith("fuera del JSON."));
        assertTrue(system.contains("- setting:"));
        assertTrue(system.contains("- cover:"));
        assertTrue(system.contains("distintos de los del primer beat"));
        assertTrue(system.contains("cast, beats, cover"));
        assertEquals(8192, ((Map<?, ?>) sent.get("parameters")).get("num_ctx"));
    }
}
