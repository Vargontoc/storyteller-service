package es.vargontoc.storyteller.application.service;

import es.vargontoc.storyteller.application.ports.in.character.PortraitGenerationUseCase;
import es.vargontoc.storyteller.application.ports.out.PortraitCandidateAssetStorage;
import es.vargontoc.storyteller.application.ports.out.PortraitGenerationTaskRegistry;
import es.vargontoc.storyteller.application.ports.out.external.PortraitGeneratorPort;
import es.vargontoc.storyteller.application.ports.out.persistence.PortraitCandidateStore;
import es.vargontoc.storyteller.application.ports.out.persistence.PortraitGenerationJobStore;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaRepository;
import java.util.List;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class PortraitGenerationService implements PortraitGenerationUseCase {
    private static final String START_FAILURE = "No se pudo iniciar la generacion de retratos.";
    private static final String GENERATION_FAILURE = "No se pudieron generar los retratos.";

    private final PortraitGenerationJobStore jobs;
    private final PortraitCandidateStore candidates;
    private final PortraitGeneratorPort generator;
    private final PortraitCandidateAssetStorage assets;
    private final PortraitGenerationTaskRegistry tasks;
    private final PortraitWebsocketNotifier notifier;
    private final CatalogCharacterJpaRepository characters;
    private final TaskExecutor executor;

    public PortraitGenerationService(PortraitGenerationJobStore jobs, PortraitCandidateStore candidates,
            PortraitGeneratorPort generator, PortraitCandidateAssetStorage assets,
            PortraitGenerationTaskRegistry tasks, PortraitWebsocketNotifier notifier,
            CatalogCharacterJpaRepository characters, TaskExecutor executor) {
        this.jobs = jobs;
        this.candidates = candidates;
        this.generator = generator;
        this.assets = assets;
        this.tasks = tasks;
        this.notifier = notifier;
        this.characters = characters;
        this.executor = executor;
    }

    @Override
    public void generate(Long catalogCharacterId) {
        var job = jobs.start(catalogCharacterId);
        tasks.register(job.id());
        try {
            notifier.started(catalogCharacterId);
            executor.execute(() -> run(job.id(), catalogCharacterId));
        } catch (RuntimeException exception) {
            tasks.unregister(job.id());
            jobs.fail(job.id(), START_FAILURE);
            notifyFailure(catalogCharacterId, START_FAILURE);
        }
    }

    private void run(Long jobId, Long catalogCharacterId) {
        try {
            notifyProgress(catalogCharacterId);
            var visualDescription = characters.findById(catalogCharacterId)
                    .orElseThrow()
                    .getVisualDescription();
            List<byte[]> generated = generator.generatePortraits(visualDescription);
            if (generated == null || generated.size() != 4) {
                throw new IllegalStateException("El generador no devolvio cuatro retratos");
            }
            var assetPaths = generated.stream()
                    .map(content -> assets.saveCandidateAsset(catalogCharacterId, content))
                    .toList();
            candidates.replace(catalogCharacterId, jobId, assetPaths);
            jobs.complete(jobId);
            notifySuccess(catalogCharacterId);
        } catch (RuntimeException exception) {
            cleanupFailedGeneration(jobId);
            jobs.fail(jobId, GENERATION_FAILURE);
            notifyFailure(catalogCharacterId, GENERATION_FAILURE);
        } finally {
            tasks.unregister(jobId);
        }
    }

    private void cleanupFailedGeneration(Long jobId) {
        try {
            candidates.deleteByGenerationJobId(jobId);
        } catch (RuntimeException ignored) {
            // El estado FAILED se conserva aunque la limpieza secundaria falle.
        }
    }

    private void notifyFailure(Long catalogCharacterId, String reason) {
        try {
            notifier.error(catalogCharacterId, reason);
        } catch (RuntimeException ignored) {
            // El fallo de difusion no debe dejar el trabajo bloqueado.
        }
    }

    private void notifySuccess(Long catalogCharacterId) {
        try {
            notifier.success(catalogCharacterId);
        } catch (RuntimeException ignored) {
            // La difusión no revierte un lote que ya se ha persistido como completado.
        }
    }

    private void notifyProgress(Long catalogCharacterId) {
        try {
            notifier.progress(catalogCharacterId);
        } catch (RuntimeException ignored) {
            // El trabajo continúa aunque el cliente no reciba un evento intermedio.
        }
    }
}
