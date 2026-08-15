package es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto;

import es.vargontoc.storyteller.domain.model.CharacterReviewTarget;

public record ReviewCharacterRequestDto(CharacterReviewTarget target, String hint) { }
