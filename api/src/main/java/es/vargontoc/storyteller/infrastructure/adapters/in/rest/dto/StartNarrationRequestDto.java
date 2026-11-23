package es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto;

/**
 * Cuerpo para iniciar o renarrar un cuento.
 *
 * @param voiceId id numerico de una voz subida, o {@code "system"} para la voz del sistema
 */
public record StartNarrationRequestDto(String voiceId) { }
