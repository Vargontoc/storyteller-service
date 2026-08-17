package es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto;

import es.vargontoc.storyteller.domain.model.KindImage;

public record GenerateImageRequestDto(KindImage image, Long id) { }
