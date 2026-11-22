package es.vargontoc.storyteller.domain.response;

/** Contenido aceptado de una página (texto null en la portada) e indicación de si tiene imagen importada. */
public record AcceptedContent(String text, String scene, boolean hasImage) { }
