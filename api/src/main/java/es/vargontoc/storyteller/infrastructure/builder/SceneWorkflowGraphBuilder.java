package es.vargontoc.storyteller.infrastructure.builder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import es.vargontoc.storyteller.application.utils.RegionMaskGenerator;
import es.vargontoc.storyteller.domain.request.SceneRequest;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.clients.ComfyUIClient;
import es.vargontoc.storyteller.infrastructure.config.ComfyUIProperties;
import es.vargontoc.storyteller.infrastructure.config.ComfyUIProperties.LoraSpec;

public class SceneWorkflowGraphBuilder {
    
    private final ComfyUIProperties properties;
    private final ComfyUIClient client;

    public SceneWorkflowGraphBuilder(ComfyUIProperties properties, ComfyUIClient client) {
        this.properties = properties;
        this.client = client;
    }

    public Map<String, Object> buildScene(SceneRequest request, long seed)  throws IOException {
        Map<String, Object> graph = new LinkedHashMap<>();
        
        graph.put("4", node("CheckpointLoaderSimple", Map.of("ckpt_name", properties.checkpointName())));

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

        String trigger = properties.loras().stream().map(LoraSpec::triggerWord).filter(t -> t != null && !t.isBlank()).collect(Collectors.joining(", "));
        String triggerSuffix = trigger.isBlank() ? "": ", " + trigger;
        String styleSuffix = ", " + properties.stylePrefix() + triggerSuffix;

        graph.put("5", node("EmptyLatentImage", Map.of("width", properties.pageWidth(), "height", properties.pageHeight(), "batch_size", 1)));


        // -- Masks
        Path tmpDir = Files.createTempDirectory("scene-masks");
        Path[] masks = RegionMaskGenerator.generateMasks(properties.pageWidth(), properties.pageHeight(), 30, tmpDir);
        String maskLLeft = client.upload(masks[0]);
        String maskRight = client.upload(masks[1]);

        graph.put("106", node("LoadImage", Map.of("image", maskLLeft)));
        graph.put("107", node("ImageToMask", Map.of("image", ref("106", 0), "channel", "red")));

        graph.put("108", node("LoadImage", Map.of("image", maskRight)));
        graph.put("109", node("ImageToMask", Map.of("image", ref("108", 0), "channel", "red")));
        

        // -- Characters

        String text1 = "(%s:1.3), %s, %s%s".formatted(request.actor1Visual(), request.actor1Action(), request.background(), styleSuffix);
        String text2 = "(%s:1.3), %s, %s%s".formatted(request.actor2Visual(), request.actor2Action(), request.background(), styleSuffix);
        
        graph.put("60", node("CLIPTextEncode", Map.of("text", text1, "clip", ref(clipId, 1))));
        graph.put("61", node("ConditioningSetMask", Map.of(
            "conditioning", ref("60", 0),
            "mask", ref("107", 0),
            "strength", 1.0,
            "set_cond_area", "mask bounds"
        )));

        graph.put("62", node("CLIPTextEncode", Map.of("text", text2, "clip", ref(clipId, 1))));
        graph.put("63", node("ConditioningSetMask", Map.of(
            "conditioning", ref("62", 0),
            "mask", ref("109", 0),
            "strength", 1.0,
            "set_cond_area", "mask bounds"
        )));

        graph.put("64", node("ConditioningCombine", Map.of("conditioning_1", ref("61", 0), "conditioning_2", ref("63", 0))));
        
        
        graph.put("7", node("CLIPTextEncode", Map.of("text", properties.pageNegativePrompt(), "clip", ref(clipId, 1))));
 
        // --- IPAdapter encadenado, cada uno con su máscara ---
        graph.put("20", node("IPAdapterUnifiedLoader", Map.of(
            "preset", properties.ipadapterPreset(), "model", ref(clipId, 0))));
 
        String img1File = client.upload(request.actor1path());
        graph.put("100", node("LoadImage", Map.of("image", img1File)));
        graph.put("101", node("IPAdapterAdvanced", Map.of(
            "weight", 0.5, "weight_type", "linear", "combine_embeds", "concat", "embeds_scaling", "V only",
            "start_at", 0.0, "end_at", 0.6,
            "model", ref("20", 0), "ipadapter", ref("20", 1), "image", ref("100", 0), "attn_mask", ref("107", 0)
        )));
 
        String img2File = client.upload(request.actor2path());
        graph.put("102", node("LoadImage", Map.of("image", img2File)));
        graph.put("103", node("IPAdapterAdvanced", Map.of(
            "weight", 0.5, "weight_type", "linear", "combine_embeds", "concat", "embeds_scaling", "V only",
            "start_at", 0.0, "end_at", 0.6,
            "model", ref("101", 0), "ipadapter", ref("20", 1), "image", ref("102", 0), "attn_mask", ref("109", 0)
        )));
 
        graph.put("3", node("KSampler", Map.of(
            "seed", seed, "steps", properties.steps(), "cfg", properties.cfg(),
            "sampler_name", properties.samplerName(), "scheduler", properties.scheduler(), "denoise", 1.0,
            "model", ref("103", 0), "positive", ref("64", 0), "negative", ref("7", 0), "latent_image", ref("5", 0)
        )));
        graph.put("8", node("VAEDecode", Map.of("samples", ref("3", 0), "vae", ref("4", 2))));
        graph.put("9", node("SaveImage", Map.of("filename_prefix", "page_scene", "images", ref("8", 0))));
        
        return graph;
    }

    

    public Map<String, Object> build(String positivePrompt, String negativePrompt, int width, int height, long seed, List<Path> references, boolean removeBackground) {
        Map<String, Object> graph = new LinkedHashMap<>();

        graph.put("4", node("CheckpointLoaderSimple", Map.of("ckpt_name", properties.checkpointName())));

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
            graph.put("20", node("IPAdapterUnifiedLoader", Map.of("preset", properties.ipadapterPreset(), "model", ref(previousModelRef, previousOutput))));            previousModelRef = "20";

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
            
        }
        
        graph.put("3", node("KSampler", Map.of(
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
        )));

        String imageSave = "8";
        graph.put("8", node("VAEDecode", Map.of("samples", ref("3", 0), "vae", ref("4", 2))));
        if(removeBackground){
            graph.put("14", node("Image Remove Background (rembg)", Map.of(
                "model_name", "isnet-anime",
                "image", ref("8", 0)
            )));
            imageSave = "14";
        }
        graph.put("9", node("SaveImage", Map.of("filename_prefix", "scene", "images", ref(imageSave, 0))));

        return graph;
    }

    public  Map<String, Object> buildRefinementPass(String previousResultFilename, String globalPositiveText, long seed) {
        Map<String, Object> graph = new LinkedHashMap<>();
 
        graph.put("4", node("CheckpointLoaderSimple", Map.of("ckpt_name", properties.checkpointName())));
 
        String lastLoraId = "4";
        int loraNodeId = 30;
        for (LoraSpec lora : properties.loras()) {
            String id = String.valueOf(loraNodeId++);
            graph.put(id, node("LoraLoader", Map.of(
                "lora_name", lora.name(),
                "strength_model", lora.strengthModel(),
                "strength_clip", lora.strengthClip(),
                "model", ref(lastLoraId, 0),
                "clip", ref(lastLoraId, 1)
            )));
            lastLoraId = id;
        }
        String clipId = lastLoraId;
 
        String trigger = properties.loras().stream()
            .map(LoraSpec::triggerWord)
            .filter(t -> t != null && !t.isBlank())
            .collect(java.util.stream.Collectors.joining(", "));
        String triggerSuffix = trigger.isBlank() ? "" : ", " + trigger;
 
        graph.put("50", node("LoadImage", Map.of("image", previousResultFilename)));
        graph.put("51", node("VAEEncode", Map.of("pixels", ref("50", 0), "vae", ref("4", 2))));
 
        graph.put("6", node("CLIPTextEncode", Map.of(
            "text", globalPositiveText + ", " + properties.stylePrefix() + triggerSuffix,
            "clip", ref(clipId, 1))));
        graph.put("7", node("CLIPTextEncode", Map.of("text", properties.pageNegativePrompt(), "clip", ref(clipId, 1))));
 
        graph.put("3", node("KSampler", Map.of(
            "seed", seed, "steps", properties.steps(), "cfg", properties.cfg(),
            "sampler_name", properties.samplerName(), "scheduler", properties.scheduler(),
            "denoise", 0.3, // validado: mantiene estructura/identidad, unifica luz y textura
            "model", ref(clipId, 0), "positive", ref("6", 0), "negative", ref("7", 0), "latent_image", ref("51", 0)
        )));
        graph.put("8", node("VAEDecode", Map.of("samples", ref("3", 0), "vae", ref("4", 2))));
        graph.put("9", node("SaveImage", Map.of("filename_prefix", "page_scene_refined", "images", ref("8", 0))));
 
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
