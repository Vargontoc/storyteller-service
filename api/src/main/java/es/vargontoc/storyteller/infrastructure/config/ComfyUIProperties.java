package es.vargontoc.storyteller.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties( prefix = "app.comfy")
public record ComfyUIProperties (
    String baseUrl,
    long pollIntervalMs,
    long timeoutSeconds,

    String workflowCharacterTemplate,
    String stylePrefix,
    String characterFramingPrompt,
    String negativePrompt,

    String workflowPageTemplate,
    String pageNegativePrompt,
    
    int pageHeight,
    int pageWidth,
    
    int coverWidth,
    int coverHeight,
    String coverFramingPrompt,

    String loraName,
    double loraStrengthModel,
    double loraStrengthClip,
    String styleTriggerWord)
{}
