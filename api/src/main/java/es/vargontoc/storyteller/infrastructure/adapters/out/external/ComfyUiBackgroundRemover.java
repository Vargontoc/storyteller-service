package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import es.vargontoc.storyteller.application.ports.out.external.CharacterBackgroundRemover;
import es.vargontoc.storyteller.domain.model.ImageRef;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.clients.ComfyUIClient;
import es.vargontoc.storyteller.infrastructure.config.ComfyUIProperties;

/**
 * Elimina el fondo con el nodo rembg de ComfyUI ("Image Remove Background (rembg)",
 * el mismo que ya usa el workflow de retratos; custom node WAS Node Suite). No reescala la imagen, no pide
 * turno a generation-gate y no reintenta: cualquier fallo devuelve vacío.
 */
@Component
public class ComfyUiBackgroundRemover implements CharacterBackgroundRemover {

    static final String WORKFLOW_PATH = "comfyui/workflow-remove-background.json";
    private static final String DEFAULT_MODEL = "isnet-anime";
    private static final Logger log = LoggerFactory.getLogger(ComfyUiBackgroundRemover.class);

    private final ComfyUIClient client;
    private final ComfyUIProperties properties;
    private final ObjectMapper mapper = new ObjectMapper();

    public ComfyUiBackgroundRemover(ComfyUIClient client, ComfyUIProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public Optional<byte[]> removeBackground(byte[] portrait) {
        try {
            String uploaded = client.upload(portrait, "story-portrait.png");
            String promptId = client.queuePrompt(workflow(uploaded));
            List<ImageRef> images = awaitImages(promptId);
            if (images == null || images.isEmpty()) {
                log.warn("ComfyUI no devolvió imagen al quitar el fondo del retrato");
                return Optional.empty();
            }
            return Optional.ofNullable(client.viewImage(images.get(0)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            log.warn("No se pudo quitar el fondo del retrato: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String workflow(String uploadedName) throws Exception {
        ObjectNode graph = (ObjectNode) mapper.readTree(new ClassPathResource(WORKFLOW_PATH).getInputStream());
        ((ObjectNode) graph.path("1").path("inputs")).put("image", uploadedName);
        ((ObjectNode) graph.path("2").path("inputs")).put("model_name", backgroundModel());
        return mapper.writeValueAsString(graph);
    }

    private String backgroundModel() {
        String model = properties.characterBackgroundModel();
        return model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();
    }

    /** Sondea hasta que ComfyUI termina o se agota el timeout; null si se agota. */
    private List<ImageRef> awaitImages(String promptId) throws InterruptedException {
        long deadline = System.nanoTime() + properties.timeoutSeconds() * 1_000_000_000L;
        while (System.nanoTime() < deadline) {
            List<ImageRef> images = client.pollHistory(promptId);
            if (images != null) {
                return images;
            }
            Thread.sleep(properties.pollIntervalMs());
        }
        return null;
    }
}
