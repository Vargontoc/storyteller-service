package es.vargontoc.storyteller.infrastructure.adapters.in.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.vargontoc.storyteller.domain.enums.NarrationEventType;
import es.vargontoc.storyteller.domain.enums.StoryPageEventType;
import es.vargontoc.storyteller.domain.events.NarrationEvent;
import es.vargontoc.storyteller.domain.events.PortraitGenerationEvent;
import es.vargontoc.storyteller.domain.events.StoryPageGenerationEvent;
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

    @Test
    void broadcastsStoryPageEventsToSubscribers() throws Exception {
        WebsocketAdapter adapter = new WebsocketAdapter();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(UUID.randomUUID().toString());
        adapter.afterConnectionEstablished(session);

        adapter.sendStoryPageNotification(new StoryPageGenerationEvent(StoryPageEventType.VERSION_PRESENTED, 7L, 0, null));
        adapter.sendStoryPageNotification(new StoryPageGenerationEvent(StoryPageEventType.HINT_REJECTED, 7L, 2, "Fuera del beat"));

        ArgumentCaptor<TextMessage> message = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, times(2)).sendMessage(message.capture());
        JsonNode presented = mapper.readTree(message.getAllValues().get(0).getPayload());
        assertEquals("VERSION_PRESENTED", presented.get("type").asText());
        assertEquals(7L, presented.get("storyId").asLong());
        assertEquals(0, presented.get("pageNumber").asInt());
        assertFalse(presented.has("reason"));
        JsonNode rejected = mapper.readTree(message.getAllValues().get(1).getPayload());
        assertEquals("HINT_REJECTED", rejected.get("type").asText());
        assertEquals("Fuera del beat", rejected.get("reason").asText());
        adapter.afterConnectionClosed(session, CloseStatus.NORMAL);
    }

    @Test
    void broadcastsNarrationEventsToSubscribers() throws Exception {
        WebsocketAdapter adapter = new WebsocketAdapter();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(UUID.randomUUID().toString());
        adapter.afterConnectionEstablished(session);

        adapter.sendNarrationNotification(new NarrationEvent(NarrationEventType.STARTED, 7L, null));
        adapter.sendNarrationNotification(new NarrationEvent(NarrationEventType.COMPLETED, 7L, "Abuela"));
        adapter.sendNarrationNotification(new NarrationEvent(NarrationEventType.FAILED, 7L, null));

        ArgumentCaptor<TextMessage> message = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, times(3)).sendMessage(message.capture());
        JsonNode started = mapper.readTree(message.getAllValues().get(0).getPayload());
        assertEquals("STARTED", started.get("type").asText());
        assertEquals(7L, started.get("storyId").asLong());
        assertFalse(started.has("voiceName"));
        JsonNode completed = mapper.readTree(message.getAllValues().get(1).getPayload());
        assertEquals("COMPLETED", completed.get("type").asText());
        assertEquals(7L, completed.get("storyId").asLong());
        assertEquals("Abuela", completed.get("voiceName").asText());
        JsonNode failed = mapper.readTree(message.getAllValues().get(2).getPayload());
        assertEquals("FAILED", failed.get("type").asText());
        assertEquals(7L, failed.get("storyId").asLong());
        assertFalse(failed.has("voiceName"));
        adapter.afterConnectionClosed(session, CloseStatus.NORMAL);
    }
}
