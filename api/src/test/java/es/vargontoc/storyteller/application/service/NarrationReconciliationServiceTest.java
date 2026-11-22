package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import es.vargontoc.storyteller.application.ports.out.GenerationGatePort.GenerationType;
import es.vargontoc.storyteller.application.ports.out.persistence.NarrationRepository;
import es.vargontoc.storyteller.domain.enums.NarrationEventType;
import es.vargontoc.storyteller.domain.enums.NarrationStatus;
import es.vargontoc.storyteller.domain.events.NarrationEvent;
import es.vargontoc.storyteller.domain.model.StoryNarration;

/** Repositorio en memoria y publicador que registra los eventos; sin Spring ni base de datos. */
class NarrationReconciliationServiceTest {

    private final Map<Long, StoryNarration> rows = new LinkedHashMap<>();
    private final List<NarrationEvent> events = new ArrayList<>();

    private final NarrationRepository repository = new NarrationRepository() {
        public StoryNarration markInProgress(Long storyId) {
            StoryNarration n = new StoryNarration(storyId, NarrationStatus.IN_PROGRESS, null, List.of());
            rows.put(storyId, n);
            return n;
        }
        public Optional<StoryNarration> find(Long storyId) { return Optional.ofNullable(rows.get(storyId)); }
        public List<StoryNarration> findInProgress() {
            return rows.values().stream().filter(n -> n.getStatus() == NarrationStatus.IN_PROGRESS).toList();
        }
        public void publish(Long storyId, String voiceName, Map<Integer, byte[]> audiosByPage) {
            rows.put(storyId, new StoryNarration(storyId, NarrationStatus.COMPLETED, voiceName,
                    List.copyOf(audiosByPage.keySet())));
        }
        public Optional<byte[]> findAudio(Long storyId, int page) { return Optional.empty(); }
        public void markError(Long storyId) {
            rows.put(storyId, new StoryNarration(storyId, NarrationStatus.ERROR, null, List.of()));
        }
        public void delete(Long storyId) { rows.remove(storyId); }
    };

    private final NarrationReconciliationService service =
            new NarrationReconciliationService(repository, new NarrationNotifier(events::add));

    @Test
    void inProgressNarrationBecomesErrorWithoutAudioNorVoiceAndNotifiesFailure() {
        repository.markInProgress(7L);

        service.reconcile();

        StoryNarration narration = repository.find(7L).orElseThrow();
        assertEquals(NarrationStatus.ERROR, narration.getStatus());
        assertFalse(narration.hasAudio());
        assertNull(narration.getVoiceName());
        assertEquals(1, events.size());
        assertEquals(NarrationEventType.FAILED, events.get(0).type());
        assertEquals(7L, events.get(0).storyId());
    }

    @Test
    void completedNarrationsAreNotTouched() {
        repository.markInProgress(8L);
        repository.publish(8L, "Abuela", Map.of(1, new byte[] {1}, 2, new byte[] {2}));

        service.reconcile();

        StoryNarration narration = repository.find(8L).orElseThrow();
        assertEquals(NarrationStatus.COMPLETED, narration.getStatus());
        assertEquals("Abuela", narration.getVoiceName());
        assertTrue(narration.hasAudio());
        assertTrue(events.isEmpty());
    }

    @Test
    void onlyInProgressOnesAreFailedWhenThereAreSeveral() {
        repository.markInProgress(1L);
        repository.markInProgress(2L);
        repository.publish(2L, "Abuela", Map.of(1, new byte[] {1}));
        repository.markInProgress(3L);

        service.reconcile();

        assertEquals(NarrationStatus.ERROR, repository.find(1L).orElseThrow().getStatus());
        assertEquals(NarrationStatus.COMPLETED, repository.find(2L).orElseThrow().getStatus());
        assertEquals(NarrationStatus.ERROR, repository.find(3L).orElseThrow().getStatus());
        assertEquals(List.of(1L, 3L), events.stream().map(NarrationEvent::storyId).toList());
    }

    @Test
    void afterReconcilingANewGenerationObtainsTheTurn() {
        repository.markInProgress(7L);

        service.reconcile();

        // La puerta no se persiste: tras el reinicio es una instancia nueva y libre.
        GenerationGate gate = new GenerationGate();
        assertTrue(gate.tryAcquire(GenerationType.WRITER, 7L));
        gate.release();
        assertTrue(gate.tryAcquire(GenerationType.PORTRAITS, null));
    }

    @Test
    void aFailingNotificationDoesNotStopTheRest() {
        repository.markInProgress(1L);
        repository.markInProgress(2L);
        NarrationReconciliationService failing = new NarrationReconciliationService(repository,
                new NarrationNotifier(event -> { throw new IllegalStateException("sin websocket"); }));

        failing.reconcile();

        assertEquals(NarrationStatus.ERROR, repository.find(1L).orElseThrow().getStatus());
        assertEquals(NarrationStatus.ERROR, repository.find(2L).orElseThrow().getStatus());
    }
}
