package es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto;

import es.vargontoc.storyteller.domain.enums.CharacterReviewTarget;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReviewCharacterRequestDto(@NotNull CharacterReviewTarget target, @NotBlank String hint, boolean replacePending) { }
