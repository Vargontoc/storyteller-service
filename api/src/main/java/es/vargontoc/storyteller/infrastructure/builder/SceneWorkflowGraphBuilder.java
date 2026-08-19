package es.vargontoc.storyteller.infrastructure.builder;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import es.vargontoc.storyteller.infrastructure.adapters.in.rest.clients.ComfyUIClient;
import es.vargontoc.storyteller.infrastructure.config.ComfyUIProperties;

public class SceneWorkflowGraphBuilder {
    
    private final ComfyUIProperties properties;
    private final ComfyUIClient client;

    public SceneWorkflowGraphBuilder(ComfyUIProperties properties, ComfyUIClient client) {
        this.properties = properties;
        this.client = client;
    }

    public Map<String, Object> build(String positivePrompt, String negativePrompt, int width, int height, long seed, List<Path> references) {
        Map<String, Object> graph = new LinkedHashMap<>();

        graph.put("4", node("CheckpointLoaderSimple", Map.of("ckpt_name", "dreamshaper_8.safetensors")));
        
        graph.put("10", node("LoraLoader", Map.of(
            "lora_name", properties.loraName(),
            "strength_model", (properties.loraStrengthModel()),
            "strength_clip", properties.loraStrengthClip(),
            "model", ref("4", 0),
            "clip", ref("4", 1)
        )));

        graph.put("5", node("EmptyLatentImage", Map.of(
            "width", width,
            "height", height,
            "batch_size", 1
        )));

        graph.put("6", node("CLIPTextEncode", Map.of(
            "text", positivePrompt,
            "clip", ref("10", 1)
        )));

        graph.put("7", node("CLIPTextEncode", Map.of(
            "text", negativePrompt,
            "clip", ref("10", 1)
        )));

        graph.put("20", node("IPAdapterUnifiedLoader", Map.of(
            "preset", "STANDARD (medium strength)",
            "model", ref("10", 0)
        )));


        String previousModel = "20";
        int previousOutput = 0;
        double weightReference = references.isEmpty() ? 0.0 : properties.ipadapterWeightBudget() / references.size();
        int nodeId = 100;

        for (Path image : references) {
            String filename = client.upload(image);

            String loadedId = String.valueOf(nodeId++);
            graph.put(loadedId, node("LoadImage", Map.of(
                "image", filename
            )));

            String applyId = String.valueOf(nodeId++);
            graph.put(applyId, node("IPAdapterAdvanced", Map.of(
                "weight", weightReference,
                "weight_type", "linear",
                "combine_embeds", "avarage",
                "start_at", 0.0,
                "end_at", 1.0,
                "model", ref(previousModel, previousOutput),
                "ipadapter", ref("20", 1),
                "image", ref(loadedId, 0)
            )));

            previousModel = applyId;
            previousOutput = 0;
        }

        graph.put("3", node("KSampler", Map.of(
            "seed", seed,
            "steps", 28,
            "cfg", 6.5,
            "sampler_name", "dpmp_2m",
            "scheduler", "karras",
            "denoise", 1.0,
            "model", ref(previousModel,previousOutput),
            "positive", ref("6", 0),
            "negative", ref("7", 0),
            "latent_image",ref("5", 0)
        )));

        graph.put("8", node("VAEDecode", Map.of(
            "samples", ref("3", 0),
            "vae", ref("4", 2))));

        graph.put("9", node("SaveImage", Map.of(
            "filename_prefix", "scene",
            "images", ref("8", 0)
        )));


        return graph;
    }

    private Map<String,Object> node(String classType, Map<String, Object> inputs) {
        Map<String, Object> n = new LinkedHashMap<>();
        n.put("class_type", classType);
        n.put("inputs", inputs);
        return n;
    }

    private List<Object> ref(String nodeId, int outputIndex) {
        return List.of(nodeId, outputIndex);
    }
}
