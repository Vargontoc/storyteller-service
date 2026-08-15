package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.application.ports.out.external.ImageGeneratorPort;
import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.domain.model.ImageRef;
import es.vargontoc.storyteller.domain.request.ImageGenerationRequest;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.clients.ComfyUIClient;
import es.vargontoc.storyteller.infrastructure.config.ComfyUIProperties;
import es.vargontoc.storyteller.infrastructure.storage.WorkflowTemplateLoader;
import es.vargontoc.storyteller.shared.exceptions.AppException;

@Component
public class ImageGeneratorAdapter implements ImageGeneratorPort {

    private final ComfyUIClient client;
    private final WorkflowTemplateLoader loader;
    private final OllamaPort ollama;
    private final ComfyUIProperties config;

    public ImageGeneratorAdapter(ComfyUIClient client, WorkflowTemplateLoader loader, OllamaPort ollama,
            ComfyUIProperties config) {
        this.client = client;
        this.loader = loader;
        this.ollama = ollama;
        this.config = config;
    }

    @Override
    public byte[] generateImage(ImageGenerationRequest request) {
        // 1. Detenemos los servicios activos en ollama
        ollama.stopAllServices();
        
        // 2. Formamos el prompt visual
        String positivePrompt = config.stylePrefix() +", " + request.visualDescription();
        long seed = request.seed() != null ? request.seed() : System.nanoTime();

        // 3. Cargamos el workflow
        String workflow = loader.render(positivePrompt, config.negativePrompt(), seed);
        String promptId = client.queuePrompt(workflow);

        // 4. Comprobamos las imagenes
        List<ImageRef> images = pollUnitReady(promptId);
        if(images == null || images.isEmpty())
            throw new AppException("ComfyUI terminó sin generar imagenes para el prompt_id: " + promptId, HttpStatus.NOT_FOUND);

        // 5. Devolvemos la imagen
        return client.viewImage(images.get(0));
    }

    private List<ImageRef> pollUnitReady(String promptId) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(config.timeoutSeconds()));

        while(Instant.now().isBefore(deadline)){
            List<ImageRef> images = client.pollHistory(promptId);
            if(images != null)
                return images;
            sleep(config.pollIntervalMs());
        }
        throw new AppException("timeout esperando a ComfyUI (prompt_id=" + promptId + ", " + config.timeoutSeconds() + ")", HttpStatus.BAD_REQUEST);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException("Interrumpido esperando a ComfyUI", HttpStatus.BAD_REQUEST);
        }
    }

    
    
    
}
