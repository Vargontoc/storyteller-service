package es.vargontoc.storyteller.infrastructure.dto;

import es.vargontoc.storyteller.domain.CharacterReviewTarget;

public record ReviewCharacterRequestDto(CharacterReviewTarget target, String hint) { }
