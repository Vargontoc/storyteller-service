package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import es.vargontoc.storyteller.application.ports.out.PortraitCandidateAssetStorage;
import es.vargontoc.storyteller.application.ports.out.PortraitGenerationTaskRegistry;
import es.vargontoc.storyteller.application.ports.out.external.PortraitGeneratorPort;
import es.vargontoc.storyteller.application.ports.out.persistence.PortraitCandidateStore;
import es.vargontoc.storyteller.application.ports.out.persistence.PortraitGenerationJobStore;
import es.vargontoc.storyteller.domain.enums.PortraitGenerationStatus;
import es.vargontoc.storyteller.domain.model.ActorMetadata;
import es.vargontoc.storyteller.domain.model.CharacterPortraitPrompt;
import es.vargontoc.storyteller.domain.model.PortraitGenerationJob;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaRepository;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import es.vargontoc.storyteller.application.ports.out.GenerationGatePort.GenerationType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;

class PortraitGenerationServiceTest {
    @Test
    void queuesThenStoresExactlyFourCandidatesAndCompletesTheJob() {
        var fixture = fixture();
        var executor = new RecordingExecutor();
        var service = fixture.service(executor);
        var character = new CatalogCharacterJpaEntity();
        character.setVisualDescription("zorra naranja exploradora");
        character.setMetadata(new ActorMetadata("small orange fox explorer", List.of("small orange fox", "fluffy white-tipped tail")));
        when(fixture.characters.findById(7L)).thenReturn(Optional.of(character));
        when(fixture.generator.generatePortraits(any(CharacterPortraitPrompt.class)))
                .thenReturn(List.of(new byte[] {1}, new byte[] {2}, new byte[] {3}, new byte[] {4}));
        when(fixture.assets.saveCandidateAsset(eq(7L), org.mockito.ArgumentMatchers.any(byte[].class)))
                .thenReturn("one.png", "two.png", "three.png", "four.png");

        service.generate(7L);

        assertNotNull(executor.task, "La solicitud se encola sin ejecutar ComfyUI en el hilo de llamada");
        InOrder order = inOrder(fixture.registry, fixture.notifier);
        order.verify(fixture.registry).register(15L);
        order.verify(fixture.notifier).started(7L);
        executor.run();

        ArgumentCaptor<CharacterPortraitPrompt> prompt = ArgumentCaptor.forClass(CharacterPortraitPrompt.class);
        verify(fixture.generator).generatePortraits(prompt.capture());
        assertEquals("zorra naranja exploradora", prompt.getValue().visualDescription());
        assertEquals("small orange fox explorer", prompt.getValue().visualDescriptionEn());
        assertEquals(List.of("small orange fox", "fluffy white-tipped tail"), prompt.getValue().visualAttributes());
        verify(fixture.candidates).replace(7L, 15L, List.of("one.png", "two.png", "three.png", "four.png"));
        verify(fixture.jobs).complete(15L);
        verify(fixture.notifier).progress(7L);
        verify(fixture.notifier).success(7L);
        verify(fixture.registry).unregister(15L);
    }

    @Test
    void incompleteGenerationFailsCleansOnlyItsCycleAndReleasesTheRegistry() {
        var fixture = fixture();
        var executor = new RecordingExecutor();
        var service = fixture.service(executor);
        var character = new CatalogCharacterJpaEntity();
        character.setVisualDescription("zorra");
        when(fixture.characters.findById(7L)).thenReturn(Optional.of(character));
        when(fixture.generator.generatePortraits(any(CharacterPortraitPrompt.class)))
                .thenReturn(List.of(new byte[] {1}, new byte[] {2}, new byte[] {3}));

        service.generate(7L);
        executor.run();

        verify(fixture.candidates).deleteByGenerationJobId(15L);
        verify(fixture.jobs).fail(eq(15L), anyString());
        verify(fixture.notifier).error(eq(7L), anyString());
        verify(fixture.registry).unregister(15L);
        verify(fixture.jobs, never()).complete(15L);
    }

    @Test
    void enqueueFailureFailsAndReleasesTheJobWithoutRunningGeneration() {
        var fixture = fixture();
        var service = fixture.service(task -> { throw new IllegalStateException("executor unavailable"); });

        service.generate(7L);

        verify(fixture.registry).register(15L);
        verify(fixture.registry).unregister(15L);
        verify(fixture.jobs).fail(eq(15L), anyString());
        verify(fixture.notifier).error(eq(7L), anyString());
        verify(fixture.generator, never()).generatePortraits(any(CharacterPortraitPrompt.class));
    }

    @Test
    void generatorFailureFailsCleansItsCycleAndReleasesTheRegistry() {
        var fixture = fixture();
        var executor = new RecordingExecutor();
        var service = fixture.service(executor);
        var character = new CatalogCharacterJpaEntity();
        character.setVisualDescription("zorra");
        when(fixture.characters.findById(7L)).thenReturn(Optional.of(character));
        when(fixture.generator.generatePortraits(any(CharacterPortraitPrompt.class)))
                .thenThrow(new IllegalStateException("ComfyUI no disponible"));

        service.generate(7L);
        executor.run();

        verify(fixture.candidates).deleteByGenerationJobId(15L);
        verify(fixture.jobs).fail(eq(15L), anyString());
        verify(fixture.notifier).error(eq(7L), anyString());
        verify(fixture.registry).unregister(15L);
    }

    @Test
    void curatedAppFailureReasonIsPropagatedToTheJobAndTheClient() {
        var fixture = fixture();
        var executor = new RecordingExecutor();
        var service = fixture.service(executor);
        var character = new CatalogCharacterJpaEntity();
        character.setVisualDescription("zorra");
        when(fixture.characters.findById(7L)).thenReturn(Optional.of(character));
        when(fixture.generator.generatePortraits(any(CharacterPortraitPrompt.class)))
                .thenThrow(new AppException("ComfyUI devolvió un retrato sin fondo transparente.", HttpStatus.BAD_GATEWAY));

        service.generate(7L);
        executor.run();

        verify(fixture.jobs).fail(15L, "ComfyUI devolvió un retrato sin fondo transparente.");
        verify(fixture.notifier).error(7L, "ComfyUI devolvió un retrato sin fondo transparente.");
    }

    @Test
    void aSecondRequestIsRejectedByTheRunningJobStoreWithoutSchedulingAnotherJob() {
        var fixture = fixture();
        var executor = new RecordingExecutor();
        var service = fixture.service(executor);
        when(fixture.jobs.start(7L)).thenThrow(new IllegalStateException("Ya hay una generación en curso"));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> service.generate(7L));

        assertNull(executor.task);
        verify(fixture.registry, never()).register(15L);
        verify(fixture.notifier, never()).started(7L);
    }

    @Test
    void completedRegenerationsReplaceCandidatesForEachNewJob() {
        var fixture = fixture();
        var executor = new RecordingExecutor();
        var service = fixture.service(executor);
        var character = new CatalogCharacterJpaEntity();
        character.setVisualDescription("zorra");
        when(fixture.jobs.start(7L)).thenReturn(
                new PortraitGenerationJob(15L, 7L, PortraitGenerationStatus.RUNNING, null),
                new PortraitGenerationJob(16L, 7L, PortraitGenerationStatus.RUNNING, null));
        when(fixture.characters.findById(7L)).thenReturn(Optional.of(character));
        when(fixture.generator.generatePortraits(any(CharacterPortraitPrompt.class))).thenReturn(fourImages());
        when(fixture.assets.saveCandidateAsset(eq(7L), org.mockito.ArgumentMatchers.any(byte[].class)))
                .thenReturn("a", "b", "c", "d", "e", "f", "g", "h");

        service.generate(7L);
        executor.run();
        service.generate(7L);
        executor.run();

        verify(fixture.candidates).replace(7L, 15L, List.of("a", "b", "c", "d"));
        verify(fixture.candidates).replace(7L, 16L, List.of("e", "f", "g", "h"));
        verify(fixture.jobs).complete(15L);
        verify(fixture.jobs).complete(16L);
    }

    @Test
    void gateIsReleasedWhenTheJobEndsInSuccessOrError() {
        var fixture = fixture();
        var executor = new RecordingExecutor();
        var service = fixture.service(executor);
        var character = new CatalogCharacterJpaEntity();
        character.setVisualDescription("zorra");
        when(fixture.characters.findById(7L)).thenReturn(Optional.of(character));
        when(fixture.generator.generatePortraits(any(CharacterPortraitPrompt.class)))
                .thenReturn(fourImages()).thenThrow(new IllegalStateException("ComfyUI no disponible"));

        fixture.gate.tryAcquire(GenerationType.PORTRAITS, null);
        service.generate(7L);
        assertFalse(fixture.gate.tryAcquire(GenerationType.WRITER, null));
        executor.run();
        assertTrue(fixture.gate.tryAcquire(GenerationType.WRITER, null));

        fixture.gate.release();
        fixture.gate.tryAcquire(GenerationType.PORTRAITS, null);
        service.generate(7L);
        executor.run();
        assertTrue(fixture.gate.tryAcquire(GenerationType.WRITER, null));
    }

    @Test
    void gateIsReleasedWhenTheJobCannotBeQueued() {
        var fixture = fixture();
        var service = fixture.service(task -> { throw new IllegalStateException("executor unavailable"); });
        fixture.gate.tryAcquire(GenerationType.PORTRAITS, null);

        service.generate(7L);

        assertTrue(fixture.gate.tryAcquire(GenerationType.WRITER, null));
    }

    private List<byte[]> fourImages() {
        return List.of(new byte[] {1}, new byte[] {2}, new byte[] {3}, new byte[] {4});
    }

    private Fixture fixture() {
        var jobs = mock(PortraitGenerationJobStore.class);
        when(jobs.start(7L)).thenReturn(new PortraitGenerationJob(15L, 7L, PortraitGenerationStatus.RUNNING, null));
        return new Fixture(jobs, mock(PortraitCandidateStore.class), mock(PortraitGeneratorPort.class),
                mock(PortraitCandidateAssetStorage.class), mock(PortraitGenerationTaskRegistry.class),
                mock(PortraitWebsocketNotifier.class), mock(CatalogCharacterJpaRepository.class), new GenerationGate());
    }

    private record Fixture(PortraitGenerationJobStore jobs, PortraitCandidateStore candidates,
            PortraitGeneratorPort generator, PortraitCandidateAssetStorage assets,
            PortraitGenerationTaskRegistry registry, PortraitWebsocketNotifier notifier,
            CatalogCharacterJpaRepository characters, GenerationGate gate) {
        PortraitGenerationService service(TaskExecutor executor) {
            return new PortraitGenerationService(jobs, candidates, generator, assets, registry, notifier, characters, executor, gate);
        }
    }

    private static final class RecordingExecutor implements TaskExecutor {
        private Runnable task;

        @Override
        public void execute(Runnable task) {
            this.task = task;
        }

        void run() {
            task.run();
        }
    }
}
