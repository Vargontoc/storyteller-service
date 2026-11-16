package es.vargontoc.storyteller.infrastructure.adapters.in.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.vargontoc.storyteller.domain.events.PortraitGenerationEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class WebsocketAdapterTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void broadcastsPortraitEventsThroughTheExistingPublicHandler() throws Exception {
        WebsocketAdapter adapter = new WebsocketAdapter();
        WebSocketSession session = mock(WebSocketSession.class);
        String sessionId = UUID.randomUUID().toString();
        when(session.getId()).thenReturn(sessionId);
        adapter.afterConnectionEstablished(session);

        adapter.sendPortraitNotification(PortraitGenerationEvent.success(42L));

        ArgumentCaptor<TextMessage> message = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(message.capture());
        JsonNode payload = mapper.readTree(message.getValue().getPayload());
        assertEquals(42L, payload.get("catalogCharacterId").asLong());
        assertEquals("SUCCESS", payload.get("type").asText());
        assertEquals("COMPLETED", payload.get("status").asText());
        adapter.afterConnectionClosed(session, CloseStatus.NORMAL);
    }
}
