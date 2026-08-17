package es.vargontoc.storyteller.domain.request;

public record AudioGenerationRequest(
    String text,
    String voiceName,
    double exageration,
    double cfgWeight,
    double temperature
) {
    
}
