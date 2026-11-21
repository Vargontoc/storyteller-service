package es.vargontoc.storyteller.domain.response;

import es.vargontoc.storyteller.domain.model.SceneComposition;

/**
 * Respuesta del escritor a un hint: si lo acepta, trae la nueva versión (texto y escena);
 * si lo rechaza, solo el motivo.
 */
public record WriterHintResult(
        boolean hintAccepted,
        String rejectionReason,
        String text,
        SceneComposition scene) { }
