package es.vargontoc.storyteller.domain.response;

import es.vargontoc.storyteller.domain.enums.PageStatus;

/** Resultado de descartar las propuestas de una portada o página reabierta: la página sigue con su versión aceptada. */
public record ProposalsDiscarded(Long storyId, Long pageId, PageStatus status) { }
