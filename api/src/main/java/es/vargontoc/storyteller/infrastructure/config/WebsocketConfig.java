package es.vargontoc.storyteller.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import es.vargontoc.storyteller.application.ports.in.WebsocketUseCase;

@Configuration
@EnableWebSocket
public class WebsocketConfig implements WebSocketConfigurer {

    private final WebsocketUseCase websocketUseCase;

    public WebsocketConfig(WebsocketUseCase websocketUseCase) {
        this.websocketUseCase = websocketUseCase;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(websocketUseCase, "/storyteller")
            .setAllowedOriginPatterns("*");
    }
}
