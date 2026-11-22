package es.vargontoc.storyteller.domain.response;

import es.vargontoc.storyteller.domain.enums.PageStatus;

/**
 * Resultado de aceptar la propuesta vigente de una portada o página reabierta.
 * Sin confirmar y con páginas posteriores: {@code requiresConfirmation} es true, {@code affectedPages} cuántas se eliminarían
 * y no ha cambiado nada. Aplicada: {@code requiresConfirmation} es false y {@code affectedPages} cuántas se eliminaron.
 */
public record ProposalAccepted(Long storyId, Long pageId, int pageNumber, PageStatus status,
                               boolean requiresConfirmation, int affectedPages) { }
