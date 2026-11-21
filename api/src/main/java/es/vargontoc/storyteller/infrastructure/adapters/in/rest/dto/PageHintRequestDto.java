package es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto;

/** Hint del usuario para cambiar la portada o una página pendiente; la obligatoriedad la valida el servicio. */
public record PageHintRequestDto(String hint) { }
