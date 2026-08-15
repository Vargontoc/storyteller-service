package es.vargontoc.storyteller.domain;

public record ImageGenerationRequest(
    String visualDescription,
    Long seed
) { }
