package es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto;

import es.vargontoc.storyteller.domain.enums.KindImage;

public record GenerateImageRequestDto(KindImage image, Long id) { }
