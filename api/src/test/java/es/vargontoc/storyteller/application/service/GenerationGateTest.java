package es.vargontoc.storyteller.application.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static es.vargontoc.storyteller.application.ports.out.GenerationGatePort.GenerationType.CHARACTER_AGENT;
import static es.vargontoc.storyteller.application.ports.out.GenerationGatePort.GenerationType.DIRECTOR;
import static es.vargontoc.storyteller.application.ports.out.GenerationGatePort.GenerationType.PORTRAITS;
import static es.vargontoc.storyteller.application.ports.out.GenerationGatePort.GenerationType.WRITER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerationGateTest {

    private final GenerationGate gate = new GenerationGate();

    @Test
    void newGateIsFree() {
        assertTrue(gate.tryAcquire(WRITER, 1L));
    }

    @Test
    void writerOfOtherStoryIsDenied() {
        assertTrue(gate.tryAcquire(WRITER, 1L));
        assertFalse(gate.tryAcquire(WRITER, 2L));
    }

    @Test
    void writerIsDeniedWhilePortraitsRun() {
        assertTrue(gate.tryAcquire(PORTRAITS, null));
        assertFalse(gate.tryAcquire(WRITER, 1L));
    }

    @Test
    void characterAgentIsDeniedWhileDirectorRuns() {
        assertTrue(gate.tryAcquire(DIRECTOR, 1L));
        assertFalse(gate.tryAcquire(CHARACTER_AGENT, null));
    }

    @Test
    void releaseAfterSuccessGrantsNext() {
        assertTrue(gate.tryAcquire(WRITER, 1L));
        gate.release();
        assertTrue(gate.tryAcquire(WRITER, 2L));
    }

    @Test
    void releaseInFinallyAfterFailureGrantsNext() {
        try {
            assertTrue(gate.tryAcquire(WRITER, 1L));
            throw new IllegalStateException("fallo simulado");
        } catch (IllegalStateException ignored) {
            // el servicio real libera en finally
        } finally {
            gate.release();
        }
        assertTrue(gate.tryAcquire(DIRECTOR, 1L));
    }

    @Test
    void asksWhetherCurrentGenerationBelongsToStory() {
        gate.tryAcquire(WRITER, 1L);
        assertTrue(gate.isGeneratingStory(1L));
        assertFalse(gate.isGeneratingStory(2L));
    }

    @Test
    void portraitsGenerationBelongsToNoStory() {
        gate.tryAcquire(PORTRAITS, null);
        assertFalse(gate.isGeneratingStory(1L));
        assertFalse(gate.isGeneratingStory(null));
    }

    @Test
    void freeGateBelongsToNoStory() {
        assertFalse(gate.isGeneratingStory(1L));
    }

    @Test
    void simultaneousRequestsGrantOnlyOne() throws Exception {
        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger granted = new AtomicInteger();
        try {
            java.util.List<Future<?>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    if (gate.tryAcquire(WRITER, 1L)) {
                        granted.incrementAndGet();
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> f : futures) {
                f.get();
            }
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, granted.get());
    }
}
