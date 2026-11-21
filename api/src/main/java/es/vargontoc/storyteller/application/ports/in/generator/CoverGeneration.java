package es.vargontoc.storyteller.application.ports.in.generator;

import es.vargontoc.storyteller.domain.response.GenerationStarted;

/** Generación de la escena de portada de un cuento con escaleta. */
public interface CoverGeneration {

    /** Valida, pide turno y devuelve el inicio; la portada se crea después, de forma asíncrona. */
    GenerationStarted startCover(Long storyId);
}
