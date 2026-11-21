package es.vargontoc.storyteller.application.ports.in.generator;

import es.vargontoc.storyteller.domain.response.GenerationStarted;

/** Generación de la página N de un cuento con escaleta desde su beat. */
public interface PageGeneration {

    /** Valida la secuencia, pide turno y devuelve el inicio; la página se crea después, de forma asíncrona. */
    GenerationStarted startPage(Long storyId, int number);
}
