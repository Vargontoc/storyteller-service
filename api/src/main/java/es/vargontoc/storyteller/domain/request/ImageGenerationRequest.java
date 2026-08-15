package es.vargontoc.storyteller.domain.request;

public record ImageGenerationRequest(
    String visualDescription,
    Long seed
) { }
