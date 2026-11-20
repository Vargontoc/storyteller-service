package es.vargontoc.storyteller.application.ports.in.generator;

import es.vargontoc.storyteller.domain.response.StoryScript;

/** Consulta de solo lectura del guion vigente (escaleta) de un cuento. */
public interface StoryScriptReading {

    /** Guion del cuento; lanza ResourceNotFoundException si no existe. En cuentos antiguos, reparto y beats vacios. */
    StoryScript getScript(Long storyId);
}
