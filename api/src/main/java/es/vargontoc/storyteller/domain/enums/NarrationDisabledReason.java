package es.vargontoc.storyteller.domain.enums;

/** Motivo por el que narrar (o renarrar) un cuento no esta habilitado. */
public enum NarrationDisabledReason {
    /** Cuento antiguo: no ofrece narracion. */
    NO_OUTLINE,
    /** Alguna pagina interior no esta aceptada. */
    PAGES_NOT_ACCEPTED,
    /** Ya se esta narrando. */
    IN_PROGRESS
}
