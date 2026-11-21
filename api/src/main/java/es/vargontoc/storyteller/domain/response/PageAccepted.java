package es.vargontoc.storyteller.domain.response;

import es.vargontoc.storyteller.domain.enums.PageStatus;

/** Resultado de aceptar la portada (pageNumber 0) o una página. */
public record PageAccepted(Long storyId, Long pageId, int pageNumber, PageStatus status) { }
