package es.vargontoc.storyteller.infrastructure.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import es.vargontoc.storyteller.application.ports.in.WebsocketUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

class WebsocketConfigTest {
    @Test
    void registersTheExistingPublicStorytellerChannel() {
        WebsocketUseCase handler = mock(WebsocketUseCase.class);
        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
        WebSocketHandlerRegistration registration = mock(WebSocketHandlerRegistration.class);
        org.mockito.Mockito.when(registry.addHandler(handler, "/storyteller")).thenReturn(registration);
        org.mockito.Mockito.when(registration.setAllowedOriginPatterns("*")).thenReturn(registration);

        new WebsocketConfig(handler).registerWebSocketHandlers(registry);

        verify(registry).addHandler(handler, "/storyteller");
        verify(registration).setAllowedOriginPatterns("*");
    }
}
