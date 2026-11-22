package es.vargontoc.storyteller.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.mockito.ArgumentCaptor;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestClient;

class NarratorModelProvisionerTest {

    private static final String NAME = "storyteller-audio-narrator";

    private final ApplicationArguments args = mock(ApplicationArguments.class);

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

    private String modelfile() throws Exception {
        return new ClassPathResource(NarratorModelProvisioner.MODELFILE_PATH).getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void missingModelIsRegisteredWithNarratorModelfile() {
        RestClient client = mock(RestClient.class);
        RestClient.RequestBodyUriSpec request = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec body = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec response = mock(RestClient.ResponseSpec.class);
        when(client.post()).thenReturn(request);
        when(request.uri("/api/create")).thenReturn(request);
        when(request.contentType(any())).thenReturn(request);
        when(request.body(any(Object.class))).thenReturn(body);
        when(body.retrieve()).thenReturn(response);
        ArgumentCaptor<Object> sent = ArgumentCaptor.forClass(Object.class);

        new NarratorModelProvisioner(apiWith(), client, NAME,
                new ClassPathResource(NarratorModelProvisioner.MODELFILE_PATH)).run(args);

        verify(request).body(sent.capture());
        Map<?, ?> payload = (Map<?, ?>) sent.getValue();
        assertEquals(NAME, payload.get("model"));
        assertEquals("qwen2.5:14b-instruct", payload.get("from"));
        assertTrue(((String) payload.get("system")).contains("AGENTE NARRADOR"));
        verify(response).toBodilessEntity();
    }

    @Test
    void existingModelIsNotRecreated() {
        RestClient client = mock(RestClient.class);

        new NarratorModelProvisioner(apiWith(NAME + ":latest"), client, NAME,
                new ClassPathResource(NarratorModelProvisioner.MODELFILE_PATH)).run(args);

        verify(client, never()).post();
    }

    @Test
    void modelfileForbidsAlteringWords() throws Exception {
        String content = modelfile();
        assertTrue(content.contains("NO añadas, NO quites, NO reordenes y NO sustituyas palabras"));
        assertTrue(content.contains("No tengas en cuenta el número de palabras"));
    }

    @Test
    void modelfileReturnsFragmentsWithExpressivenessPerPage() throws Exception {
        String content = modelfile();
        assertTrue(content.contains("UN guion por cada página"));
        assertTrue(content.contains("\"pages\""));
        assertTrue(content.contains("\"fragments\""));
        assertTrue(content.contains("exaggeration"));
        assertTrue(content.contains("cfgWeight"));
        assertTrue(content.contains("temperature"));
        assertTrue(content.contains("frases completas"));
    }

    @Test
    void productionConstructorIsInjectable() {
        new ApplicationContextRunner()
                .withPropertyValues("spring.ai.ollama.base-url=http://localhost:11434",
                        "app.agents.audio-narrator=" + NAME,
                        "app.agents.audio-narrator-provisioning.enabled=true")
                .withBean(OllamaApi.class, () -> mock(OllamaApi.class))
                .withBean(NarratorModelProvisioner.class)
                .run(context -> {
                    assertTrue(context.isRunning());
                    assertNotNull(context.getBean(NarratorModelProvisioner.class));
                });
    }

    @Test
    void modelfileIsNotEmpty() throws Exception {
        assertFalse(modelfile().isBlank());
    }
}
