package es.vargontoc.storyteller.infrastructure.adapters.in.websocket;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.vargontoc.storyteller.application.ports.in.WebsocketUseCase;
import es.vargontoc.storyteller.domain.events.WebsocketNotificationEvent;
import es.vargontoc.storyteller.shared.exceptions.AppException;

@Component
public class WebsocketAdapter extends TextWebSocketHandler implements WebsocketUseCase {

    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        if(!sessions.containsKey(session.getId()))
            sessions.put(session.getId(), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        session.sendMessage(new TextMessage("Echo " + message.getPayload()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }

    @Override
    public void sendNotification(WebsocketNotificationEvent event) {
        if(!sessions.isEmpty()) {
            sessions.values().forEach(s -> {
                try {
                    s.sendMessage(new TextMessage(mapper.writeValueAsString(event)));
                }catch(IOException e){
                    throw new AppException("Could not sent notification", HttpStatus.INTERNAL_SERVER_ERROR);
                }
            });
        }
    }
}
