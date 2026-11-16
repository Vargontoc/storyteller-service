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
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void queuesOneWorkflowWithFourDistinctPortraitBranchesAndDownloadsAllImages() throws Exception {
        ComfyUIClient client = mock(ComfyUIClient.class);
        when(client.queuePrompt(any())).thenReturn("prompt-1");
        List<ImageRef> refs = List.of(ref("one"), ref("two"), ref("three"), ref("four"));
        when(client.pollHistory("prompt-1")).thenReturn(refs);
        for (int index = 0; index < refs.size(); index++) {
            when(client.viewImage(refs.get(index))).thenReturn(new byte[] {(byte) index});
        }

        PortraitComfyUiAdapter adapter = adapter(client);
        List<byte[]> portraits = adapter.generatePortraits("zorra naranja con cola esponjosa");

        assertEquals(4, portraits.size());
        verify(client, times(4)).viewImage(any(ImageRef.class));
        ArgumentCaptor<String> workflow = ArgumentCaptor.forClass(String.class);
        verify(client).queuePrompt(workflow.capture());
        JsonNode graph = mapper.readTree(workflow.getValue());
        assertEquals(4, graph.findValues("class_type").stream().filter(node -> "KSampler".equals(node.asText())).count());
        assertEquals(4, graph.findValues("class_type").stream().filter(node -> "SaveImage".equals(node.asText())).count());
        assertTrue(graph.toString().contains("zorra naranja con cola esponjosa"));
        assertTrue(graph.toString().contains("children's book illustration"));

        List<Long> seeds = graph.findValues("seed").stream().map(JsonNode::asLong).toList();
        assertEquals(4, seeds.stream().distinct().count());
    }

    @Test
    void rejectsAnIncompleteComfyUiResultWithoutDownloadingIt() {
        ComfyUIClient client = mock(ComfyUIClient.class);
        when(client.queuePrompt(any())).thenReturn("prompt-1");
        when(client.pollHistory("prompt-1")).thenReturn(List.of(ref("one"), ref("two"), ref("three")));

        assertThrows(AppException.class, () -> adapter(client).generatePortraits("zorra naranja"));
        verify(client, times(0)).viewImage(any(ImageRef.class));
    }

    @Test
    void propagatesTransportFailuresFromComfyUi() {
        ComfyUIClient client = mock(ComfyUIClient.class);
        ResourceAccessException failure = new ResourceAccessException("ComfyUI inaccesible");
        when(client.queuePrompt(any())).thenThrow(failure);

        assertEquals(failure, assertThrows(ResourceAccessException.class, () -> adapter(client).generatePortraits("zorra naranja")));
    }

    private PortraitComfyUiAdapter adapter(ComfyUIClient client) {
        ComfyUIProperties properties = properties();
        return new PortraitComfyUiAdapter(client, new PortraitWorkflowGraphBuilder(properties), properties);
    }

    private ComfyUIProperties properties() {
        return new ComfyUIProperties(
            "http://localhost:8188", 1, 180, "cartoon.safetensors", "euler", "normal", 20, 7.0,
            "children's book illustration", "solo full body portrait", "photo, scary", "photo", "cover",
            512, 512, 1152, 896, 896, 1152,
            List.of(new ComfyUIProperties.LoraSpec("cartoon.safetensors", 0.6, 0.6, "cute cartoon")),
            "ip", "clip", "preset", 3, 0.5
        );
    }

    private ImageRef ref(String filename) {
        return new ImageRef(filename + ".png", "", "output");
    }
}
