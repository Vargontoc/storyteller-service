package es.vargontoc.storyteller.application.service;

import es.vargontoc.storyteller.application.ports.out.GenerationGatePort;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementación en memoria de la puerta de generación. Al arrancar está libre.
 */
@Component
public class GenerationGate implements GenerationGatePort {

    private record Turn(GenerationType type, Long storyId) {
    }

    private final AtomicReference<Turn> current = new AtomicReference<>();

    @Override
    public boolean tryAcquire(GenerationType type, Long storyId) {
        return current.compareAndSet(null, new Turn(type, storyId));
    }

    @Override
    public void release() {
        current.set(null);
    }

    @Override
    public boolean isGeneratingStory(Long storyId) {
        Turn turn = current.get();
        return turn != null && turn.storyId() != null && Objects.equals(turn.storyId(), storyId);
    }
}
