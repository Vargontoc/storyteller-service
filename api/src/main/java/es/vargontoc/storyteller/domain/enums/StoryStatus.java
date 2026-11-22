package es.vargontoc.storyteller.domain.enums;

public enum StoryStatus {
    SCRIPT_PENDING,
    SCRIPT_ACCEPTED,
    /** Todas las páginas de la escaleta están aceptadas: el cuento está listo para la etapa de audio (REQ-021). */
    PAGES_ACCEPTED
}
