package es.vargontoc.storyteller.infrastructure.dto;

import es.vargontoc.storyteller.domain.StorySize;
import jakarta.validation.constraints.NotNull;

public record StoryRequestDto(
    @NotNull
    StorySize size,
    @NotNull
    Long topicId
) { }
