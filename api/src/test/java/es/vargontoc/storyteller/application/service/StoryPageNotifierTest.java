package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import es.vargontoc.storyteller.application.ports.out.StoryPageEventPublisher;
import es.vargontoc.storyteller.domain.enums.StoryPageEventType;
import es.vargontoc.storyteller.domain.events.StoryPageGenerationEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class StoryPageNotifierTest {
    private final StoryPageEventPublisher publisher = mock(StoryPageEventPublisher.class);
    private final StoryPageNotifier notifier = new StoryPageNotifier(publisher);

    @Test
    void startedCarriesTypeStoryAndPage() {
        notifier.started(1L, 3);

        assertEvent(StoryPageEventType.STARTED, 1L, 3, null);
    }

    @Test
    void versionPresentedOfTheCoverIdentifiesPageZero() {
        notifier.versionPresented(1L, 0);

        assertEvent(StoryPageEventType.VERSION_PRESENTED, 1L, 0, null);
    }

    @Test
    void hintRejectedCarriesTheWriterReason() {
        notifier.hintRejected(1L, 2, "El hint contradice el guion");

        assertEvent(StoryPageEventType.HINT_REJECTED, 1L, 2, "El hint contradice el guion");
    }

    @Test
    void failedCarriesTypeAndAffectedPage() {
        notifier.failed(1L, 4);

        assertEvent(StoryPageEventType.FAILED, 1L, 4, null);
    }

    @Test
    void rejectsEventsWithoutStoryId() {
        assertThrows(NullPointerException.class, () -> notifier.started(null, 1));
    }

    private void assertEvent(StoryPageEventType type, Long storyId, int page, String reason) {
        ArgumentCaptor<StoryPageGenerationEvent> event = forClass(StoryPageGenerationEvent.class);
        verify(publisher).publish(event.capture());
        assertEquals(type, event.getValue().type());
        assertEquals(storyId, event.getValue().storyId());
        assertEquals(page, event.getValue().pageNumber());
        if (reason == null) {
            assertNull(event.getValue().reason());
        } else {
            assertEquals(reason, event.getValue().reason());
        }
    }
}
