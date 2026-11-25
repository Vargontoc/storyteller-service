package es.vargontoc.storyteller.application.ports.in.generator;

import es.vargontoc.storyteller.domain.response.GenerationStarted;

/** Generación de la escena de portada de un cuento con escaleta. */
public interface CoverGeneration {

    /** Valida, pide turno y devuelve el inicio; la portada se crea después, de forma asíncrona. */
    GenerationStarted startCover(Long storyId);

    /**
     * Descarta la portada del cuento (versiones, contenido aceptado e imagen importada) al rehacerse el guion.
     * Sin portada no hace nada; con una generación del cuento en curso se rechaza con 409 sin tocar nada.
     */
    void discardCover(Long storyId);
}
