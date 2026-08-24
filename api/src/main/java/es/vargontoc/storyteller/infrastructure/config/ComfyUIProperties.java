package es.vargontoc.storyteller.infrastructure.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties( prefix = "app.comfy")
public record ComfyUIProperties (
    String baseUrl,
    long pollIntervalMs,
    long timeoutSeconds,

    String checkpointName,
    String samplerName,
    String scheduler,
    int steps,
    double cfg,

    String stylePrefix,
    String characterFramingPrompt,
    String characterNegativePrompt,
    String pageNegativePrompt,
    String coverFramingPrompt,
    
    int characterHeight,
    int characterWidth,

    int pageHeight,
    int pageWidth,
    
    int coverWidth,
    int coverHeight,

    List<LoraSpec> loras,

    String ipadapterModelName,
    String clipVisionModelName,
    String ipadapterPreset,
    int maxReferenceCharacters,
    double ipadapterWeightBudget
    )
{

    public record LoraSpec(String name, double strengthModel, double strengthClip, String triggerWord){}
}
