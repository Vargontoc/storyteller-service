package es.vargontoc.storyteller.domain.enums;

/** Resultado de un intento de edición directa de un beat. */
public enum BeatEditStatus {
    /** Edición aplicada: beat actualizado y guion en SCRIPT_PENDING. */
    APPLIED,
    /** El director rechaza el texto por las reglas de edad: nada cambia. */
    REJECTED,
    /** Hay páginas generadas: hay que confirmar su invalidación antes de aplicar. */
    CONFIRMATION_REQUIRED,
    /** El usuario canceló el aviso: nada cambia. */
    CANCELLED
}
