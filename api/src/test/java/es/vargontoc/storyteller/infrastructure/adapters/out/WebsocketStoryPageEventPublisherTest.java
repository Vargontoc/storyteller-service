package es.vargontoc.storyteller.infrastructure.adapters.out;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import es.vargontoc.storyteller.application.ports.in.WebsocketUseCase;
import es.vargontoc.storyteller.domain.enums.StoryPageEventType;
import es.vargontoc.storyteller.domain.events.StoryPageGenerationEvent;
import org.junit.jupiter.api.Test;

class WebsocketStoryPageEventPublisherTest {

    @Test
    void delegatesTheEventToTheWebsocketBroadcast() {
        WebsocketUseCase websocket = mock(WebsocketUseCase.class);
        StoryPageGenerationEvent event = new StoryPageGenerationEvent(StoryPageEventType.STARTED, 3L, 0, null);

        new WebsocketStoryPageEventPublisher(websocket).publish(event);

        verify(websocket).sendStoryPageNotification(event);
    }
}
