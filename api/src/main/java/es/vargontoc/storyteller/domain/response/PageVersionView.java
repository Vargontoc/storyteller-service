package es.vargontoc.storyteller.domain.response;

import es.vargontoc.storyteller.domain.enums.PageLengthWarning;

/** Versión del histórico (pendiente) o propuesta (reapertura). Texto null en la portada. Nunca lleva imagen. */
public record PageVersionView(Long id, String text, String scene, PageLengthWarning warning, boolean current) { }
