package es.vargontoc.storyteller.domain.enums;

/** Causas estables del rechazo de una voz subida (ALT-001); el nombre del valor es el codigo. */
public enum VoiceRejectionReason {
    NAME_REQUIRED,
    LANGUAGE_REQUIRED,
    PERMISSION_REQUIRED,
    NAME_DUPLICATED,
    INVALID_FORMAT,
    INVALID_DURATION,
    /** Borrado de la voz del sistema (REQ-014). */
    SYSTEM_VOICE_NOT_DELETABLE,
    /** Borrado de una voz que no existe. */
    VOICE_NOT_FOUND
}
