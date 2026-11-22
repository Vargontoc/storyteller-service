package es.vargontoc.storyteller.domain.enums;

/** Causas estables por las que no se puede iniciar una narracion; el nombre del valor es el codigo. */
public enum NarrationRejectionReason {
    /** Cuento antiguo, sin escaleta. */
    NO_OUTLINE,
    /** Alguna pagina interior no esta aceptada. */
    PAGES_NOT_ACCEPTED,
    /** El cuento ya se esta narrando. */
    IN_PROGRESS,
    /** La voz elegida no es elegible (borrada o en un idioma que no es el espanol). */
    VOICE_NOT_AVAILABLE,
    /** No hay ninguna voz elegible en espanol. */
    NO_VOICES_AVAILABLE,
    /** Otra generacion tiene el turno. */
    GENERATION_IN_PROGRESS
}
