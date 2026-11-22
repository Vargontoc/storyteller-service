package es.vargontoc.storyteller.domain.response;

import es.vargontoc.storyteller.domain.enums.PageLengthWarning;

/** Resultado de elegir una versión como vigente: la versión elegida y su aviso de longitud. */
public record VersionSelected(Long storyId, Long pageId, Long versionId, PageLengthWarning warning) { }
