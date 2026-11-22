package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import es.vargontoc.storyteller.application.ports.out.NarrationEventPublisher;
import es.vargontoc.storyteller.domain.enums.NarrationEventType;
import es.vargontoc.storyteller.domain.events.NarrationEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NarrationNotifierTest {
    private final NarrationEventPublisher publisher = mock(NarrationEventPublisher.class);
    private final NarrationNotifier notifier = new NarrationNotifier(publisher);

    @Test
    void startedCarriesTypeAndStory() {
        notifier.started(1L);

        assertEvent(NarrationEventType.STARTED, 1L, null);
    }

    @Test
    void completedCarriesStoryAndVoiceName() {
        notifier.completed(1L, "Abuela");

        assertEvent(NarrationEventType.COMPLETED, 1L, "Abuela");
    }

    @Test
    void failedCarriesTypeAndStory() {
        notifier.failed(1L);

        assertEvent(NarrationEventType.FAILED, 1L, null);
    }

    @Test
    void rejectsEventsWithoutStoryId() {
        assertThrows(NullPointerException.class, () -> notifier.started(null));
    }

    private void assertEvent(NarrationEventType type, Long storyId, String voiceName) {
        ArgumentCaptor<NarrationEvent> event = forClass(NarrationEvent.class);
        verify(publisher).publish(event.capture());
        assertEquals(type, event.getValue().type());
        assertEquals(storyId, event.getValue().storyId());
        if (voiceName == null) {
            assertNull(event.getValue().voiceName());
        } else {
            assertEquals(voiceName, event.getValue().voiceName());
        }
    }
}
