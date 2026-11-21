package es.vargontoc.storyteller.application.ports.in.generator;

import es.vargontoc.storyteller.domain.model.StoryPage;

/** Aceptación de la portada o de una página pendiente de un cuento con escaleta. */
public interface PageAcceptance {

    /** Promueve la versión vigente a contenido aceptado sin imagen y borra el histórico; no inicia ninguna generación. */
    StoryPage acceptPage(Long storyId, Long pageId);
}
