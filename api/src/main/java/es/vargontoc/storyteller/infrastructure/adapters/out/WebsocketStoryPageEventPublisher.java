package es.vargontoc.storyteller.infrastructure.adapters.out;

import es.vargontoc.storyteller.application.ports.in.WebsocketUseCase;
import es.vargontoc.storyteller.application.ports.out.StoryPageEventPublisher;
import es.vargontoc.storyteller.domain.events.StoryPageGenerationEvent;
import org.springframework.stereotype.Component;

/** Entrega los eventos de portada y página a los clientes suscritos a /storyteller. */
@Component
public class WebsocketStoryPageEventPublisher implements StoryPageEventPublisher {

    private final WebsocketUseCase websocket;

    public WebsocketStoryPageEventPublisher(WebsocketUseCase websocket) {
        this.websocket = websocket;
    }

    @Override
    public void publish(StoryPageGenerationEvent event) {
        websocket.sendStoryPageNotification(event);
    }
}
