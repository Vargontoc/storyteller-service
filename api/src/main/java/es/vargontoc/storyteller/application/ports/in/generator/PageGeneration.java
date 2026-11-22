package es.vargontoc.storyteller.application.ports.in.generator;

import es.vargontoc.storyteller.domain.response.GenerationStarted;

/** Generación de la página N de un cuento con escaleta desde su beat. */
public interface PageGeneration {

    /** Valida la secuencia, pide turno y devuelve el inicio; la página se crea después, de forma asíncrona. */
    GenerationStarted startPage(Long storyId, int number);

    /** Cambio con hint sobre la portada o una página pendiente: valida, pide turno y devuelve el inicio; la nueva versión se crea después. */
    GenerationStarted startHint(Long storyId, Long pageId, String hint);

    /** Regeneración sin hint de una página pendiente cuya versión vigente avisa de número de frases; la nueva versión se añade al histórico. */
    GenerationStarted startRegenerate(Long storyId, Long pageId);
}
