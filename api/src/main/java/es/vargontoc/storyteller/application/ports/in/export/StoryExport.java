package es.vargontoc.storyteller.application.ports.in.export;

import es.vargontoc.storyteller.domain.response.ExportResult;

/** Exportaciones del cuento a aplicaciones externas. Se calculan al pedirlas y no se persisten. */
public interface StoryExport {

    /** Zip para Nubi Game, o todas las faltas del cuento (sin zip). 404 si el cuento no existe. */
    ExportResult exportForNubiGame(Long storyId);

    /**
     * Zip de entrada del agente visual (contrato v0.1), o todas las condiciones que faltan y las reglas
     * del contrato incumplidas (sin zip). No exige imágenes ni audio. 404 si el cuento no existe.
     */
    ExportResult exportForVisualAgent(Long storyId);
}
