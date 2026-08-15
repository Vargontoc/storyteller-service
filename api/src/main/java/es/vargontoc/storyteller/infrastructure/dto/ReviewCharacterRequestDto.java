package es.vargontoc.storyteller.infrastructure.dto;

import es.vargontoc.storyteller.domain.model.CharacterReviewTarget;

public record ReviewCharacterRequestDto(CharacterReviewTarget target, String hint) { }
