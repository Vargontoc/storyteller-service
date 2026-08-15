package es.vargontoc.storyteller.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties( prefix = "app.comfy")
public record ComfyUIProperties (
    String baseUrl,
    String workflowCharacterTemplate,
    long pollIntervalMs,
    long timeoutSeconds,
    String stylePrefix,
    String negativePrompt,
    String loraName,
    double loraStrengthModel,
    double loraStrengthClip,
    String styleTriggerWord,
    String characterFramingPrompt,
    boolean backgroundRemovalEnabled)
{}
