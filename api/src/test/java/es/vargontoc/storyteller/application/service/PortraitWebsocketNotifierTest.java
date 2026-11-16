package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import es.vargontoc.storyteller.application.ports.in.WebsocketUseCase;
import es.vargontoc.storyteller.domain.enums.PortraitGenerationEventType;
import es.vargontoc.storyteller.domain.enums.PortraitGenerationStatus;
import es.vargontoc.storyteller.domain.events.PortraitGenerationEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PortraitWebsocketNotifierTest {
    private final WebsocketUseCase websocket = mock(WebsocketUseCase.class);
    private final PortraitWebsocketNotifier notifier = new PortraitWebsocketNotifier(websocket);

    @Test
    void publishesStartProgressAndSuccessWithTheCatalogCharacterIdAndState() {
        assertEvent(() -> notifier.started(42L), PortraitGenerationEventType.STARTED, PortraitGenerationStatus.RUNNING);
        assertEvent(() -> notifier.progress(42L), PortraitGenerationEventType.PROGRESS, PortraitGenerationStatus.RUNNING);
        assertEvent(() -> notifier.success(42L), PortraitGenerationEventType.SUCCESS, PortraitGenerationStatus.COMPLETED);
    }

    @Test
    void publishesErrorWithACommunicableReasonAndFailureState() {
        notifier.error(42L, "ComfyUI no está disponible. Inténtalo de nuevo.");

        ArgumentCaptor<PortraitGenerationEvent> event = forClass(PortraitGenerationEvent.class);
        verify(websocket).sendPortraitNotification(event.capture());
        assertEquals(42L, event.getValue().catalogCharacterId());
        assertEquals(PortraitGenerationEventType.ERROR, event.getValue().type());
        assertEquals(PortraitGenerationStatus.FAILED, event.getValue().status());
        assertEquals("ComfyUI no está disponible. Inténtalo de nuevo.", event.getValue().reason());
    }

    @Test
    void rejectsEventsWithoutTheRequiredCatalogCharacterId() {
        assertThrows(NullPointerException.class, () -> PortraitGenerationEvent.started(null));
    }

    private void assertEvent(Runnable action, PortraitGenerationEventType type, PortraitGenerationStatus status) {
        clearInvocations(websocket);
        action.run();
        ArgumentCaptor<PortraitGenerationEvent> event = forClass(PortraitGenerationEvent.class);
        verify(websocket).sendPortraitNotification(event.capture());
        assertEquals(42L, event.getValue().catalogCharacterId());
        assertEquals(type, event.getValue().type());
        assertEquals(status, event.getValue().status());
    }
}
