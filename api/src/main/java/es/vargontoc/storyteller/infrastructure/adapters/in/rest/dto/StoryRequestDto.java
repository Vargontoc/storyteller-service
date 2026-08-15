package es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto;

import es.vargontoc.storyteller.domain.model.StorySize;
import jakarta.validation.constraints.NotNull;

public record StoryRequestDto(
    @NotNull
    StorySize size,
    @NotNull
    Long topicId
) { }
