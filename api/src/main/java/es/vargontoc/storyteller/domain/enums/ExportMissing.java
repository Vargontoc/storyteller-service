package es.vargontoc.storyteller.domain.enums;

/** Qué le falta a una portada, página o cuento para poder exportarlo. */
public enum ExportMissing {
    /** La portada no está aceptada o el guion no tiene su planteamiento (lugar y tono). */
    COVER_NOT_ACCEPTED,
    /** La página interior no está aceptada (no tiene texto aceptado). */
    ACCEPTED_TEXT,
    IMAGE,
    AUDIO,
    /** Agente visual: el guion no está aprobado (falta de todo el cuento, {@code page} -1). */
    SCRIPT_NOT_APPROVED,
    /** Agente visual: al beat de la página le falta el lugar de escena (guion anterior a la feature o incompleto). */
    BEAT_SETTING,
    /** Agente visual: a la portada aceptada le faltan el lugar o el tono en el guion. */
    COVER_SETTING_TONE,
    /** Agente visual: el cuento no tiene instantánea del protagonista ({@code page} -1). */
    PROTAGONIST_MISSING,
    /** Agente visual: no hay imagen del protagonista (sin retrato o no se pudo quitar el fondo; {@code page} -1). */
    PROTAGONIST_IMAGE,
    /** Agente visual: regla incumplida del contrato v0.1; el código estable va en {@code ExportFault.code} ({@code page} -1). */
    CONTRACT_RULE
}
