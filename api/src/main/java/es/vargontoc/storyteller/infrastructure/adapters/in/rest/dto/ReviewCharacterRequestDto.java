package es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto;

import es.vargontoc.storyteller.domain.enums.CharacterReviewTarget;

public record ReviewCharacterRequestDto(CharacterReviewTarget target, String hint) { }
