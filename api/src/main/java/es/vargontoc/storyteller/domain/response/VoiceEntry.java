package es.vargontoc.storyteller.domain.response;

/**
 * Voz de la biblioteca.
 *
 * @param id     id de la voz subida; null en la voz del sistema
 * @param system true en la voz del sistema
 */
public record VoiceEntry(Long id, String name, String language, boolean system) { }
