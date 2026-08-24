package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.vargontoc.storyteller.application.ports.out.external.ImageGeneratorPort;
import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.domain.enums.KindImage;
import es.vargontoc.storyteller.domain.model.ImageRef;

import es.vargontoc.storyteller.domain.request.ImageGenerationRequest;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.clients.ComfyUIClient;
import es.vargontoc.storyteller.infrastructure.builder.SceneWorkflowGraphBuilder;
import es.vargontoc.storyteller.infrastructure.config.ComfyUIProperties;
import es.vargontoc.storyteller.shared.exceptions.AppException;


@Component
public class ImageGeneratorAdapter implements ImageGeneratorPort {
    private static final double SUBJECT_EMPHASIS_WEIGHT = 1.3;
    private static final Logger LOG = LoggerFactory.getLogger(ImageGeneratorAdapter.class);
    private final ComfyUIClient client;
    private final OllamaPort ollama;
    private final ComfyUIProperties config;
    private final SceneWorkflowGraphBuilder builder;

    public ImageGeneratorAdapter(ComfyUIClient client, OllamaPort ollama,
            ComfyUIProperties config) {
        this.client = client;
        this.ollama = ollama;
        this.config = config;

        builder = new SceneWorkflowGraphBuilder(config, client);
    }

    @Override
    public byte[] generateImage(ImageGenerationRequest request) {
        
        
        // 1. Detenemos los servicios activos en ollama
        ollama.stopAllServices();


        int[] dims = getDimensions(request.kind());

            
        // 3. Obtenemos prompt positive segun tipo de peticion
        String positivePrompt = buildPositive(request);
        LOG.info("Positive: {}", positivePrompt);
        // 4. Obtenemos prompt negative
        String negativePrompt = buildNegative(request);
        LOG.info("Negative: {}", negativePrompt);
        // 5. Obtenemos seed
        long seed = request.seed() != null ? request.seed() : System.nanoTime();
            

        
        var graph = builder.build(positivePrompt, negativePrompt, dims[0], dims[1], seed, request.kind() == KindImage.ACTOR ? List.of() : request.references());
        return generate(graph);
    }

    private byte[] generate(Map<String, Object> graph) {
        try {
            ObjectMapper om = new ObjectMapper();
            String json = om.writeValueAsString(graph);
    
            String promptId = client.queuePrompt(json);
    
            // 4. Comprobamos las imagenes
            List<ImageRef> images = pollUnitReady(promptId);
            if(images == null || images.isEmpty())
                throw new AppException("ComfyUI terminó sin generar imagenes para el prompt_id: " + promptId, HttpStatus.NOT_FOUND);
    
            // 5. Devolvemos la imagen
            return client.viewImage(images.get(0));

        }catch(Exception e) {
            return new byte[]{};
        }
    }

    private int[] getDimensions(KindImage kind) {
        return switch(kind)  {
            case ACTOR -> new int[]{config.characterWidth(), config.characterHeight() };
            case PAGE -> new int[]{config.pageWidth(), config.pageHeight()};
            case COVER -> new int[]{config.coverWidth(), config.coverHeight()};
        };
    }

    private String buildNegative(ImageGenerationRequest request) {
        return switch(request.kind()) {
            case ACTOR -> config.characterNegativePrompt();
            case PAGE, COVER -> config.pageNegativePrompt();
        };
    }

    @SuppressWarnings("null")
    private String buildPositive(ImageGenerationRequest request) {
        String trigger = config.loras().stream().map(ComfyUIProperties.LoraSpec::triggerWord)
            .filter(t -> t != null && !t.isBlank()).collect(Collectors.joining(", "));
        String triggerSuffix = trigger.isBlank() ? "" : ", " + trigger;


            
        String subject = (request.kind() == KindImage.ACTOR && !request.attributes().isEmpty())
        ? request.attributes().stream()
            .map(attr -> "(%s:1.3)".formatted(attr, SUBJECT_EMPHASIS_WEIGHT))
            .collect(java.util.stream.Collectors.joining(", "))
        : "(%s:1.3)".formatted(request.visualDescription(), SUBJECT_EMPHASIS_WEIGHT);

        return switch (request.kind()){
            case ACTOR -> subject + ", " + config.stylePrefix() + ", " + config.characterFramingPrompt() + triggerSuffix;
            case PAGE -> subject + ", " + config.stylePrefix() + triggerSuffix;
            case COVER -> subject + ", "  + config.stylePrefix() + "; " + config.coverFramingPrompt() + triggerSuffix;
        };
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
