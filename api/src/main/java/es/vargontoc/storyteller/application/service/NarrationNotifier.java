package es.vargontoc.storyteller.application.service;

import es.vargontoc.storyteller.application.ports.out.NarrationEventPublisher;
import es.vargontoc.storyteller.domain.enums.NarrationEventType;
import es.vargontoc.storyteller.domain.events.NarrationEvent;
import org.springframework.stereotype.Service;

/** Publica los eventos de inicio, exito (con la voz) y error de la narracion de un cuento. */
@Service
public class NarrationNotifier {
    private final NarrationEventPublisher publisher;

    public NarrationNotifier(NarrationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void started(Long storyId) {
        publisher.publish(new NarrationEvent(NarrationEventType.STARTED, storyId, null));
    }

    public void completed(Long storyId, String voiceName) {
        publisher.publish(new NarrationEvent(NarrationEventType.COMPLETED, storyId, voiceName));
    }

    public void failed(Long storyId) {
        publisher.publish(new NarrationEvent(NarrationEventType.FAILED, storyId, null));
    }
}
