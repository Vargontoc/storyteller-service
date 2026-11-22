package es.vargontoc.storyteller.infrastructure.adapters.out;

import es.vargontoc.storyteller.application.ports.in.WebsocketUseCase;
import es.vargontoc.storyteller.application.ports.out.NarrationEventPublisher;
import es.vargontoc.storyteller.domain.events.NarrationEvent;
import org.springframework.stereotype.Component;

/** Entrega los eventos de narracion a los clientes suscritos a /storyteller. */
@Component
public class WebsocketNarrationEventPublisher implements NarrationEventPublisher {

    private final WebsocketUseCase websocket;

    public WebsocketNarrationEventPublisher(WebsocketUseCase websocket) {
        this.websocket = websocket;
    }

    @Override
    public void publish(NarrationEvent event) {
        websocket.sendNarrationNotification(event);
    }
}
