package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.vargontoc.storyteller.application.ports.out.external.PortraitGeneratorPort;
import es.vargontoc.storyteller.domain.model.ImageRef;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.clients.ComfyUIClient;
import es.vargontoc.storyteller.infrastructure.builder.PortraitWorkflowGraphBuilder;
import es.vargontoc.storyteller.infrastructure.config.ComfyUIProperties;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PortraitComfyUiAdapter implements PortraitGeneratorPort {
    private static final int PORTRAIT_COUNT = 4;

    private final ComfyUIClient client;
    private final PortraitWorkflowGraphBuilder builder;
    private final long pollIntervalMs;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PortraitComfyUiAdapter(ComfyUIClient client, PortraitWorkflowGraphBuilder builder, ComfyUIProperties properties) {
        this.client = client;
        this.builder = builder;
        this.pollIntervalMs = properties.pollIntervalMs();
    }

    @Override
    public List<byte[]> generatePortraits(String visualDescription) {
        String promptId = client.queuePrompt(workflow(visualDescription));
        List<ImageRef> generated = awaitPortraits(promptId);
        if (generated.size() != PORTRAIT_COUNT) {
            throw new AppException("ComfyUI debe devolver exactamente cuatro retratos", HttpStatus.BAD_GATEWAY);
        }
        return generated.stream().map(client::viewImage).toList();
    }

    private String workflow(String visualDescription) {
        try {
            return objectMapper.writeValueAsString(builder.build(visualDescription, System.nanoTime()));
        } catch (JsonProcessingException exception) {
            throw new AppException("No se pudo preparar el workflow de retratos", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private List<ImageRef> awaitPortraits(String promptId) {
        while (true) {
            List<ImageRef> images = client.pollHistory(promptId);
            if (images != null) {
                return images;
            }
            waitForNextPoll();
        }
    }

    private void waitForNextPoll() {
        try {
            Thread.sleep(pollIntervalMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AppException("Interrumpido esperando retratos de ComfyUI", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
