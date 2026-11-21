package es.vargontoc.storyteller.infrastructure.adapters.out;

import es.vargontoc.storyteller.application.ports.out.StoryPageEventPublisher;
import es.vargontoc.storyteller.domain.events.StoryPageGenerationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Adaptador provisional: solo registra el evento en el log. TASK-009 lo sustituye por la difusión en /storyteller (WebsocketAdapter). */
@Component
public class LoggingStoryPageEventPublisher implements StoryPageEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingStoryPageEventPublisher.class);

    @Override
    public void publish(StoryPageGenerationEvent event) {
        log.info("Evento de generación: {} historia={} página={}", event.type(), event.storyId(), event.pageNumber());
    }
}
