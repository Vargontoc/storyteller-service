package es.vargontoc.storyteller.domain.model;

import es.vargontoc.storyteller.domain.enums.PortraitGenerationStatus;

public record PortraitGenerationJob(
    Long id,
    Long catalogCharacterId,
    PortraitGenerationStatus status,
    String errorMessage
) { }
