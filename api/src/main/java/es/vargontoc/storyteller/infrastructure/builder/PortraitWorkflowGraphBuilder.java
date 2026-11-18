package es.vargontoc.storyteller.infrastructure.builder;

import es.vargontoc.storyteller.domain.model.CharacterPortraitPrompt;
import es.vargontoc.storyteller.infrastructure.config.ComfyUIProperties;
import es.vargontoc.storyteller.infrastructure.config.ComfyUIProperties.LoraSpec;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Construye un workflow de ComfyUI que entrega cuatro plantillas de personaje.
 * <p>
 * Cada resultado es un único personaje a cuerpo completo, en pose neutra, centrado
 * y con el fondo eliminado para que una aplicación externa pueda superponerlo sobre
 * las escenas del cuento. La diversidad entre candidatos se consigue con la semilla
 * y una expresión distinta, manteniendo el mismo encuadre en los cuatro.
 */
@Component
public class PortraitWorkflowGraphBuilder {
    private static final String DEFAULT_BACKGROUND_MODEL = "isnet-anime";
    private static final String OUTPUT_PREFIX = "catalog_character_template";
    private static final double ATTRIBUTE_WEIGHT = 1.2;

    private static final List<String> EXPRESSIONS = List.of(
        "friendly warm smile",
        "curious expression",
        "joyful playful smile",
        "calm confident expression"
    );

    private final ComfyUIProperties properties;

    public PortraitWorkflowGraphBuilder(ComfyUIProperties properties) {
        this.properties = properties;
    }

    public Map<String, Object> build(CharacterPortraitPrompt prompt, long firstSeed) {
        Map<String, Object> graph = new LinkedHashMap<>();
        graph.put("4", node("CheckpointLoaderSimple", Map.of("ckpt_name", properties.checkpointName())));

        String modelId = "4";
        for (int index = 0; index < properties.loras().size(); index++) {
            LoraSpec lora = properties.loras().get(index);
            String id = String.valueOf(30 + index);
            graph.put(id, node("LoraLoader", Map.of(
                "lora_name", lora.name(),
                "strength_model", lora.strengthModel(),
                "strength_clip", lora.strengthClip(),
                "model", ref(modelId, 0),
                "clip", ref(modelId, 1)
            )));
            modelId = id;
        }

        String sharedPrompt = sharedPrompt(prompt);
        graph.put("7", node("CLIPTextEncode", Map.of(
            "text", properties.characterNegativePrompt(),
            "clip", ref(modelId, 1)
        )));

        for (int index = 0; index < EXPRESSIONS.size(); index++) {
            String suffix = String.valueOf(index + 1);
            String latentId = "50" + suffix;
            String positiveId = "60" + suffix;
            String samplerId = "70" + suffix;
            String decodedId = "80" + suffix;

            graph.put(latentId, node("EmptyLatentImage", Map.of(
                "width", properties.characterWidth(),
                "height", properties.characterHeight(),
                "batch_size", 1
            )));
            graph.put(positiveId, node("CLIPTextEncode", Map.of(
                "text", sharedPrompt + ", " + EXPRESSIONS.get(index),
                "clip", ref(modelId, 1)
            )));
            graph.put(samplerId, node("KSampler", Map.of(
                "seed", firstSeed + index,
                "steps", properties.steps(),
                "cfg", properties.cfg(),
                "sampler_name", properties.samplerName(),
                "scheduler", properties.scheduler(),
                "denoise", 1.0,
                "model", ref(modelId, 0),
                "positive", ref(positiveId, 0),
                "negative", ref("7", 0),
                "latent_image", ref(latentId, 0)
            )));
            graph.put(decodedId, node("VAEDecode", Map.of(
                "samples", ref(samplerId, 0),
                "vae", ref("4", 2)
            )));

            String outputId = decodedId;
            if (properties.characterTransparentBackground()) {
                String backgroundId = "85" + suffix;
                graph.put(backgroundId, node("Image Remove Background (rembg)", Map.of(
                    "model_name", backgroundModel(),
                    "image", ref(decodedId, 0)
                )));
                outputId = backgroundId;
            }

            graph.put("90" + suffix, node("SaveImage", Map.of(
                "filename_prefix", OUTPUT_PREFIX,
                "images", ref(outputId, 0)
            )));
        }
        return graph;
    }

    private String sharedPrompt(CharacterPortraitPrompt prompt) {
        String subject = firstNonBlank(
            prompt.visualDescriptionEn(),
            prompt.visualDescription(),
            "single cartoon character"
        );
        String attributes = prompt.visualAttributes().stream()
            .filter(Objects::nonNull)
            .map(this::clean)
            .filter(attribute -> !attribute.isBlank())
            .distinct()
            .filter(attribute -> !containsIgnoreCase(subject, attribute))
            .map(attribute -> String.format(Locale.ROOT, "(%s:%.1f)", attribute, ATTRIBUTE_WEIGHT))
            .collect(Collectors.joining(", "));

        return joinPromptParts(
            subject,
            attributes,
            properties.stylePrefix(),
            properties.characterFramingPrompt(),
            triggerWords()
        );
    }

    private String triggerWords() {
        return properties.loras().stream()
            .map(LoraSpec::triggerWord)
            .filter(trigger -> trigger != null && !trigger.isBlank())
            .collect(Collectors.joining(", "));
    }

    private String backgroundModel() {
        return firstNonBlank(properties.characterBackgroundModel(), DEFAULT_BACKGROUND_MODEL);
    }

    private String firstNonBlank(String... values) {
        return Arrays.stream(values)
            .filter(Objects::nonNull)
            .map(this::clean)
            .filter(value -> !value.isBlank())
            .findFirst()
            .orElse("");
    }

    private String joinPromptParts(String... parts) {
        return Arrays.stream(parts)
            .filter(Objects::nonNull)
            .map(this::clean)
            .filter(part -> !part.isBlank())
            .collect(Collectors.joining(", "))
            .replaceAll("(,\\s*){2,}", ", ");
    }

    private boolean containsIgnoreCase(String text, String candidate) {
        return clean(text).toLowerCase(Locale.ROOT).contains(clean(candidate).toLowerCase(Locale.ROOT));
    }

    private String clean(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private Map<String, Object> node(String classType, Map<String, Object> inputs) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("class_type", classType);
        node.put("inputs", inputs);
        return node;
    }

    private List<Object> ref(String nodeId, int outputIndex) {
        return List.of(nodeId, outputIndex);
    }
}
