package es.vargontoc.storyteller.application.ports.in;

import org.springframework.web.socket.WebSocketHandler;

import es.vargontoc.storyteller.domain.events.WebsocketNotificationEvent;
import es.vargontoc.storyteller.domain.events.PortraitGenerationEvent;

public interface WebsocketUseCase extends WebSocketHandler {

    void sendNotification(WebsocketNotificationEvent notification);

    void sendPortraitNotification(PortraitGenerationEvent notification);
}
