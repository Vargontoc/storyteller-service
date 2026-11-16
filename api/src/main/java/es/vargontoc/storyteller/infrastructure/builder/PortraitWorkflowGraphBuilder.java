package es.vargontoc.storyteller.infrastructure.builder;

import es.vargontoc.storyteller.infrastructure.config.ComfyUIProperties;
import es.vargontoc.storyteller.infrastructure.config.ComfyUIProperties.LoraSpec;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PortraitWorkflowGraphBuilder {
    private static final List<String> VARIATIONS = List.of(
        "friendly smile, relaxed pose",
        "curious expression, gentle wave",
        "joyful expression, playful pose",
        "calm expression, confident pose"
    );

    private final ComfyUIProperties properties;

    public PortraitWorkflowGraphBuilder(ComfyUIProperties properties) {
        this.properties = properties;
    }

    public Map<String, Object> build(String visualDescription, long firstSeed) {
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

        String triggerWords = properties.loras().stream()
            .map(LoraSpec::triggerWord)
            .filter(trigger -> trigger != null && !trigger.isBlank())
            .collect(Collectors.joining(", "));
        String sharedPrompt = "%s, %s, %s".formatted(visualDescription, properties.stylePrefix(), properties.characterFramingPrompt());
        if (!triggerWords.isBlank()) {
            sharedPrompt += ", " + triggerWords;
        }

        graph.put("7", node("CLIPTextEncode", Map.of("text", properties.characterNegativePrompt(), "clip", ref(modelId, 1))));
        for (int index = 0; index < VARIATIONS.size(); index++) {
            String suffix = String.valueOf(index + 1);
            String latentId = "50" + suffix;
            String positiveId = "60" + suffix;
            String samplerId = "70" + suffix;
            String decodedId = "80" + suffix;
            String savedId = "90" + suffix;

            graph.put(latentId, node("EmptyLatentImage", Map.of(
                "width", properties.characterWidth(), "height", properties.characterHeight(), "batch_size", 1
            )));
            graph.put(positiveId, node("CLIPTextEncode", Map.of("text", sharedPrompt + ", " + VARIATIONS.get(index), "clip", ref(modelId, 1))));
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
            graph.put(decodedId, node("VAEDecode", Map.of("samples", ref(samplerId, 0), "vae", ref("4", 2))));
            graph.put(savedId, node("SaveImage", Map.of("filename_prefix", "catalog_portrait", "images", ref(decodedId, 0))));
        }
        return graph;
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
