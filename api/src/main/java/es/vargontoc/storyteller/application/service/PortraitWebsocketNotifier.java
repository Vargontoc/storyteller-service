package es.vargontoc.storyteller.application.service;

import es.vargontoc.storyteller.application.ports.in.WebsocketUseCase;
import es.vargontoc.storyteller.domain.events.PortraitGenerationEvent;
import org.springframework.stereotype.Service;

@Service
public class PortraitWebsocketNotifier {
    private final WebsocketUseCase websocket;

    public PortraitWebsocketNotifier(WebsocketUseCase websocket) {
        this.websocket = websocket;
    }

    public void started(Long catalogCharacterId) {
        websocket.sendPortraitNotification(PortraitGenerationEvent.started(catalogCharacterId));
    }

    public void progress(Long catalogCharacterId) {
        websocket.sendPortraitNotification(PortraitGenerationEvent.progress(catalogCharacterId));
    }

    public void success(Long catalogCharacterId) {
        websocket.sendPortraitNotification(PortraitGenerationEvent.success(catalogCharacterId));
    }

    public void error(Long catalogCharacterId, String publicReason) {
        websocket.sendPortraitNotification(PortraitGenerationEvent.error(catalogCharacterId, publicReason));
    }
}
