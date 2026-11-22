package es.vargontoc.storyteller.domain.enums;

/**
 * Acciones que el servidor ofrece sobre la portada o una página de un cuento con escaleta.
 * Reabrir una página aceptada es pedir un cambio con hint (HINT); no hay ninguna acción de edición manual.
 */
public enum PageAction {
    /** POST pages/{id}/accept: aceptar la vigente de una página pendiente. */
    ACCEPT,
    /** POST pages/{id}/hint: pedir un cambio con hint (también reabre una página aceptada). */
    HINT,
    /** POST pages/{id}/regenerate: regenerar sin hint cuando la vigente avisa del número de frases. */
    REGENERATE,
    /** POST pages/{id}/versions/{versionId}/select: elegir otra versión o propuesta como vigente. */
    SELECT_VERSION,
    /** POST pages/{id}/accept-proposal: aceptar la propuesta vigente de una página reabierta. */
    ACCEPT_PROPOSAL,
    /** POST pages/{id}/proposals/discard: descartar las propuestas de una página reabierta. */
    DISCARD_PROPOSALS
}
