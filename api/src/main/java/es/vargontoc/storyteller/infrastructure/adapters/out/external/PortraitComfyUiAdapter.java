package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.vargontoc.storyteller.application.ports.out.external.PortraitGeneratorPort;
import es.vargontoc.storyteller.domain.model.CharacterPortraitPrompt;
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
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private final ComfyUIClient client;
    private final PortraitWorkflowGraphBuilder builder;
    private final long pollIntervalMs;
    private final boolean requireTransparentBackground;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PortraitComfyUiAdapter(ComfyUIClient client, PortraitWorkflowGraphBuilder builder, ComfyUIProperties properties) {
        this.client = client;
        this.builder = builder;
        this.pollIntervalMs = properties.pollIntervalMs();
        this.requireTransparentBackground = properties.characterTransparentBackground();
    }

    @Override
    public List<byte[]> generatePortraits(CharacterPortraitPrompt prompt) {
        String promptId = client.queuePrompt(workflow(prompt));
        List<ImageRef> generated = awaitPortraits(promptId);
        if (generated.size() != PORTRAIT_COUNT) {
            throw new AppException("ComfyUI debe devolver exactamente cuatro retratos", HttpStatus.BAD_GATEWAY);
        }
        return generated.stream()
                .map(client::viewImage)
                .map(this::requireTransparentTemplate)
                .toList();
    }

    private String workflow(CharacterPortraitPrompt prompt) {
        try {
            return objectMapper.writeValueAsString(builder.build(prompt, System.nanoTime()));
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

    /**
     * La aplicación externa superpone el personaje sobre la escena, así que un retrato
     * sin canal alfa rompería la integración aunque la imagen se viese bien en ComfyUI.
     */
    private byte[] requireTransparentTemplate(byte[] content) {
        if (!requireTransparentBackground || isPngWithAlpha(content)) {
            return content;
        }
        throw new AppException(
                "ComfyUI devolvió un retrato sin fondo transparente. Revisa el nodo rembg del workflow.",
                HttpStatus.BAD_GATEWAY);
    }

    private boolean isPngWithAlpha(byte[] content) {
        if (content == null || content.length < 26) {
            return false;
        }
        for (int index = 0; index < PNG_SIGNATURE.length; index++) {
            if (content[index] != PNG_SIGNATURE[index]) {
                return false;
            }
        }
        boolean isIhdr = content[12] == 'I' && content[13] == 'H' && content[14] == 'D' && content[15] == 'R';
        if (!isIhdr) {
            return false;
        }
        int colorType = content[25] & 0xFF;
        return colorType == 4 || colorType == 6;
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
