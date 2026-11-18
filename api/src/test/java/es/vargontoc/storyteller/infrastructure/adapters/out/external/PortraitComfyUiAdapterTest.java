package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.vargontoc.storyteller.domain.model.CharacterPortraitPrompt;
import es.vargontoc.storyteller.domain.model.ImageRef;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.clients.ComfyUIClient;
import es.vargontoc.storyteller.infrastructure.builder.PortraitWorkflowGraphBuilder;
import es.vargontoc.storyteller.infrastructure.config.ComfyUIProperties;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.ResourceAccessException;

class PortraitComfyUiAdapterTest {
    private static final CharacterPortraitPrompt PROMPT = new CharacterPortraitPrompt(
            "zorra naranja con cola esponjosa",
            "small orange fox with a fluffy white-tipped tail",
            List.of("small orange fox", "fluffy white-tipped tail"));

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void queuesOneWorkflowWithFourTransparentTemplatesAndDownloadsAllImages() throws Exception {
        ComfyUIClient client = mock(ComfyUIClient.class);
        when(client.queuePrompt(any())).thenReturn("prompt-1");
        List<ImageRef> refs = List.of(ref("one"), ref("two"), ref("three"), ref("four"));
        when(client.pollHistory("prompt-1")).thenReturn(refs);
        for (int index = 0; index < refs.size(); index++) {
            when(client.viewImage(refs.get(index))).thenReturn(png(6));
        }

        PortraitComfyUiAdapter adapter = adapter(client);
        List<byte[]> portraits = adapter.generatePortraits(PROMPT);

        assertEquals(4, portraits.size());
        verify(client, times(4)).viewImage(any(ImageRef.class));
        ArgumentCaptor<String> workflow = ArgumentCaptor.forClass(String.class);
        verify(client).queuePrompt(workflow.capture());
        JsonNode graph = mapper.readTree(workflow.getValue());
        assertEquals(4, countClassType(graph, "KSampler"));
        assertEquals(4, countClassType(graph, "SaveImage"));
        assertEquals(4, countClassType(graph, "Image Remove Background (rembg)"));
        assertTrue(graph.toString().contains("small orange fox with a fluffy white-tipped tail"));
        assertTrue(graph.toString().contains("children's book illustration"));
        assertTrue(graph.toString().contains("transparent background"));
        assertTrue(graph.toString().contains("isnet-anime"));

        List<Long> seeds = graph.findValues("seed").stream().map(JsonNode::asLong).toList();
        assertEquals(4, seeds.stream().distinct().count());
    }

    @Test
    void rejectsAnIncompleteComfyUiResultWithoutDownloadingIt() {
        ComfyUIClient client = mock(ComfyUIClient.class);
        when(client.queuePrompt(any())).thenReturn("prompt-1");
        when(client.pollHistory("prompt-1")).thenReturn(List.of(ref("one"), ref("two"), ref("three")));

        assertThrows(AppException.class, () -> adapter(client).generatePortraits(PROMPT));
        verify(client, times(0)).viewImage(any(ImageRef.class));
    }

    @Test
    void rejectsARetratoWithoutAlphaChannelWhenTransparencyIsRequired() {
        ComfyUIClient client = mock(ComfyUIClient.class);
        when(client.queuePrompt(any())).thenReturn("prompt-1");
        List<ImageRef> refs = List.of(ref("one"), ref("two"), ref("three"), ref("four"));
        when(client.pollHistory("prompt-1")).thenReturn(refs);
        when(client.viewImage(any(ImageRef.class))).thenReturn(png(2));

        assertThrows(AppException.class, () -> adapter(client).generatePortraits(PROMPT));
    }

    @Test
    void propagatesTransportFailuresFromComfyUi() {
        ComfyUIClient client = mock(ComfyUIClient.class);
        ResourceAccessException failure = new ResourceAccessException("ComfyUI inaccesible");
        when(client.queuePrompt(any())).thenThrow(failure);

        assertEquals(failure, assertThrows(ResourceAccessException.class, () -> adapter(client).generatePortraits(PROMPT)));
    }

    private long countClassType(JsonNode graph, String classType) {
        return graph.findValues("class_type").stream().filter(node -> classType.equals(node.asText())).count();
    }

    private PortraitComfyUiAdapter adapter(ComfyUIClient client) {
        ComfyUIProperties properties = properties();
        return new PortraitComfyUiAdapter(client, new PortraitWorkflowGraphBuilder(properties), properties);
    }

    private ComfyUIProperties properties() {
        return new ComfyUIProperties(
            "http://localhost:8188", 1, 180, "cartoon.safetensors", "euler", "normal", 20, 7.0,
            "children's book illustration", "solo full body portrait, transparent background", "photo, scary", true, "isnet-anime", "photo", "cover",
            512, 512, 1152, 896, 896, 1152,
            List.of(new ComfyUIProperties.LoraSpec("cartoon.safetensors", 0.6, 0.6, "cute cartoon")),
            "ip", "clip", "preset", 3, 0.5
        );
    }

    private ImageRef ref(String filename) {
        return new ImageRef(filename + ".png", "", "output");
    }

    /**
     * Cabecera PNG mínima suficiente para que el adaptador compruebe el canal alfa:
     * firma + IHDR con profundidad 8 y el color type indicado (6 = RGBA, 2 = RGB).
     */
    private byte[] png(int colorType) {
        byte[] bytes = new byte[26];
        bytes[0] = (byte) 0x89;
        bytes[1] = 'P';
        bytes[2] = 'N';
        bytes[3] = 'G';
        bytes[4] = '\r';
        bytes[5] = '\n';
        bytes[6] = 0x1A;
        bytes[7] = '\n';
        bytes[12] = 'I';
        bytes[13] = 'H';
        bytes[14] = 'D';
        bytes[15] = 'R';
        bytes[24] = 8;
        bytes[25] = (byte) colorType;
        return bytes;
    }
}
