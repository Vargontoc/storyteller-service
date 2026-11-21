package es.vargontoc.storyteller.application.service;

import es.vargontoc.storyteller.application.ports.out.StoryPageEventPublisher;
import es.vargontoc.storyteller.domain.enums.StoryPageEventType;
import es.vargontoc.storyteller.domain.events.StoryPageGenerationEvent;
import org.springframework.stereotype.Service;

/** Publica los eventos de una generación. La portada se identifica con pageNumber 0. */
@Service
public class StoryPageNotifier {
    private final StoryPageEventPublisher publisher;

    public StoryPageNotifier(StoryPageEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void started(Long storyId, int pageNumber) {
        publish(StoryPageEventType.STARTED, storyId, pageNumber, null);
    }

    public void versionPresented(Long storyId, int pageNumber) {
        publish(StoryPageEventType.VERSION_PRESENTED, storyId, pageNumber, null);
    }

    public void hintRejected(Long storyId, int pageNumber, String reason) {
        publish(StoryPageEventType.HINT_REJECTED, storyId, pageNumber, reason);
    }

    public void failed(Long storyId, int pageNumber) {
        publish(StoryPageEventType.FAILED, storyId, pageNumber, null);
    }

    private void publish(StoryPageEventType type, Long storyId, int pageNumber, String reason) {
        publisher.publish(new StoryPageGenerationEvent(type, storyId, pageNumber, reason));
    }
}
