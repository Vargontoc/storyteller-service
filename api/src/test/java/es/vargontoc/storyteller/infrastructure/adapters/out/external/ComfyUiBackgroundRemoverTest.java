package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.ResourceAccessException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.vargontoc.storyteller.domain.model.ImageRef;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.clients.ComfyUIClient;
import es.vargontoc.storyteller.infrastructure.config.ComfyUIProperties;

class ComfyUiBackgroundRemoverTest {

    private final ComfyUIClient client = mock(ComfyUIClient.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deliversTheTransparentPngFromComfyUi() throws Exception {
        byte[] portrait = new byte[] {1, 2, 3};
        byte[] result = new byte[] {9, 8, 7};
        ImageRef ref = new ImageRef("cutout.png", "", "output");
        when(client.upload(portrait, "story-portrait.png")).thenReturn("story-portrait.png");
        when(client.queuePrompt(anyString())).thenReturn("p1");
        when(client.pollHistory("p1")).thenReturn(null, List.of(ref));
        when(client.viewImage(ref)).thenReturn(result);

        Optional<byte[]> out = remover(5).removeBackground(portrait);

        assertArrayEquals(result, out.orElseThrow());
        ArgumentCaptor<String> workflow = ArgumentCaptor.forClass(String.class);
        verify(client).queuePrompt(workflow.capture());
        JsonNode graph = mapper.readTree(workflow.getValue());
        assertEquals("story-portrait.png", graph.at("/1/inputs/image").asText());
        assertEquals("Image Remove Background (rembg)", graph.at("/2/class_type").asText());
        assertEquals("isnet-anime", graph.at("/2/inputs/model_name").asText());
    }

    @Test
    void doesNotResizeThePortrait() throws Exception {
        ImageRef ref = new ImageRef("cutout.png", "", "output");
        byte[] same768 = new byte[768 * 4];
        when(client.upload(any(byte[].class), anyString())).thenReturn("in.png");
        when(client.queuePrompt(anyString())).thenReturn("p1");
        when(client.pollHistory("p1")).thenReturn(List.of(ref));
        when(client.viewImage(ref)).thenReturn(same768);

        Optional<byte[]> out = remover(5).removeBackground(new byte[10]);

        assertTrue(out.orElseThrow() == same768);
        ArgumentCaptor<String> workflow = ArgumentCaptor.forClass(String.class);
        verify(client).queuePrompt(workflow.capture());
        assertFalse(workflow.getValue().toLowerCase().contains("scale"));
        assertFalse(workflow.getValue().toLowerCase().contains("resize"));
    }

    @Test
    void reportsNoImageWhenComfyUiFailsWithoutRetrying() {
        when(client.upload(any(byte[].class), anyString())).thenReturn("in.png");
        when(client.queuePrompt(anyString())).thenThrow(new ResourceAccessException("caído"));

        assertTrue(remover(5).removeBackground(new byte[10]).isEmpty());
        verify(client, times(1)).queuePrompt(anyString());
    }

    @Test
    void reportsNoImageWhenComfyUiReturnsNoOutputs() {
        when(client.upload(any(byte[].class), anyString())).thenReturn("in.png");
        when(client.queuePrompt(anyString())).thenReturn("p1");
        when(client.pollHistory("p1")).thenReturn(List.of());

        assertTrue(remover(5).removeBackground(new byte[10]).isEmpty());
        verify(client, never()).viewImage(any(ImageRef.class));
    }

    @Test
    void reportsNoImageWhenTheWorkflowNeverFinishes() {
        when(client.upload(any(byte[].class), anyString())).thenReturn("in.png");
        when(client.queuePrompt(anyString())).thenReturn("p1");
        when(client.pollHistory("p1")).thenReturn(null);

        assertTrue(remover(0).removeBackground(new byte[10]).isEmpty());
    }

    private ComfyUiBackgroundRemover remover(long timeoutSeconds) {
        ComfyUIProperties properties = new ComfyUIProperties(
            "http://localhost:8188", 1, timeoutSeconds, "ckpt", "euler", "normal", 20, 7.0,
            "style", "framing", "neg", true, "isnet-anime", "photo", "cover",
            512, 512, 1152, 896, 896, 1152,
            List.of(), "ip", "clip", "preset", 3, 0.5);
        return new ComfyUiBackgroundRemover(client, properties);
    }
}
