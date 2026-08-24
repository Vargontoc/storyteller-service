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

        graph.put("4", node("CheckpointLoadersimple", Map.of("ckpt_name", properties.checkpointName())));

        String lastLoraNodeId = "4";
        int loraNodeId = 30;

        for(var lora : properties.loras()) {
            String id = String.valueOf(loraNodeId++);

            graph.put(id, node("LoraLoader", Map.of(
                "lora_name", lora.name(),
                "strength_model", lora.strengthModel(),
                "strength_clip", lora.strengthClip(),
                "model" , ref(lastLoraNodeId, 0),
                "clip" , ref(lastLoraNodeId, 1) 
            )));

            lastLoraNodeId = id;
        }

        String clipId = lastLoraNodeId;

        graph.put("5", node("EmptyLatentImage", Map.of("width", width, "height", height, "batch_size", 1)));
        graph.put("6", node("CLIPTextEncode", Map.of("text", positivePrompt, "clip", ref(clipId, 1))));
        graph.put("7", node("CLIPTextEncode", Map.of("text", negativePrompt, "clip", ref(clipId, 1))));

        String previousModelRef = clipId;
        int previousOutput = 0;

        if(!references.isEmpty()){
            graph.put("20", node("IPAdapterUnifiedLoader", Map.of("preset",properties.ipadapterPreset(), "model", ref("10", 0))));
            previousModelRef = "20";

            double weightPerReference = properties.ipadapterWeightBudget() / references.size();
            int nodeId = 100;
            for(Path path : references){
                String filename = client.upload(path);

                String loadImageId = String.valueOf(nodeId++);
                graph.put(loadImageId, node("LoadImage", Map.of("image", filename)));

                String applyId = String.valueOf(nodeId++);
                graph.put(applyId, node("IPAdapterAdvanced",Map.of(
                    "weight", weightPerReference,
                    "weight_type", "linear",
                    "combine_embeds", "concat",
                    "embeds_scaling", "V only",
                    "start_at", 0.0,
                    "end_at", 1.0,
                    "model", ref(previousModelRef,previousOutput),
                    "ipadapter", ref("20", 1),
                    "image", ref(loadImageId, 0)
                )));

                previousModelRef = applyId;
                previousOutput = 0;
            }

            graph.put("KSampler", Map.of(
                "seed", seed,
                "steps", properties.steps(),
                "cfg", properties.cfg(),
                "sampler_name", properties.samplerName(),
                "scheduler", properties.scheduler(),
                "denoise", 1.0,
                "model", ref(previousModelRef, previousOutput),
                "positive", ref("6", 0),
                "negative", ref("7", 0),
                "latent_image", ref("5", 0)
            ));

            graph.put("8", node("VAEDecode", Map.of("samples", ref("3", 0), "vae", ref("4", 2))));
            graph.put("9", node("SaveImage", Map.of("filename_prefix", "scene", "images", ref("8", 0))));

        }

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
