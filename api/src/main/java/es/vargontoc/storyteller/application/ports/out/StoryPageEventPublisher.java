package es.vargontoc.storyteller.application.ports.out;

import es.vargontoc.storyteller.domain.events.StoryPageGenerationEvent;

/** Entrega los eventos de generación de portada y página (la difusión por /storyteller la conecta TASK-009). */
public interface StoryPageEventPublisher {

    void publish(StoryPageGenerationEvent event);
}
