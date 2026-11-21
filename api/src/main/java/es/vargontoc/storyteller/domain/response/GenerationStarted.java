package es.vargontoc.storyteller.domain.response;

/** Respuesta REST de una generación asíncrona concedida: el resultado llega después por WebSocket. */
public record GenerationStarted(Long storyId, int pageNumber) { }
