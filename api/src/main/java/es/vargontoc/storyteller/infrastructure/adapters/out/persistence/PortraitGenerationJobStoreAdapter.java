package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import es.vargontoc.framework.persistence.jpa.mapping.EntityMapperGuard;
import es.vargontoc.framework.persistence.jpa.transaction.WriteTransactionGuard;
import es.vargontoc.storyteller.application.ports.out.persistence.PortraitGenerationJobStore;
import es.vargontoc.storyteller.domain.enums.PortraitGenerationStatus;
import es.vargontoc.storyteller.domain.model.PortraitGenerationJob;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.PortraitGenerationJobJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.PortraitGenerationJobJpaRepository;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PortraitGenerationJobStoreAdapter implements PortraitGenerationJobStore {
    private final PortraitGenerationJobJpaRepository jobs;
    private final CatalogCharacterJpaRepository catalogCharacters;

    public PortraitGenerationJobStoreAdapter(PortraitGenerationJobJpaRepository jobs,
            CatalogCharacterJpaRepository catalogCharacters) {
        this.jobs = jobs;
        this.catalogCharacters = catalogCharacters;
    }

    @Override
    @Transactional
    public PortraitGenerationJob start(Long catalogCharacterId) {
        WriteTransactionGuard.requireWriteTransaction();
        CatalogCharacterJpaEntity character = catalogCharacters.findByIdForUpdate(catalogCharacterId)
            .orElseThrow(() -> new AppException("Catálogo no encontrado", HttpStatus.NOT_FOUND));
        if (!jobs.findByCatalogCharacterIdAndStatus(catalogCharacterId, PortraitGenerationStatus.RUNNING).isEmpty()) {
            throw new AppException("El personaje ya tiene una generación de retratos en curso", HttpStatus.CONFLICT);
        }
        PortraitGenerationJobJpaEntity job = new PortraitGenerationJobJpaEntity();
        job.setCatalogCharacter(character);
        job.setStatus(PortraitGenerationStatus.RUNNING);
        return map(jobs.save(job));
    }

    @Override
    public Optional<PortraitGenerationJob> findRunningByCatalogCharacterId(Long catalogCharacterId) {
        return jobs.findByCatalogCharacterIdAndStatus(catalogCharacterId, PortraitGenerationStatus.RUNNING)
            .stream().findFirst().map(this::map);
    }

    @Override
    @Transactional
    public PortraitGenerationJob complete(Long jobId) {
        return finish(jobId, PortraitGenerationStatus.COMPLETED, null);
    }

    @Override
    @Transactional
    public PortraitGenerationJob fail(Long jobId, String publicReason) {
        return finish(jobId, PortraitGenerationStatus.FAILED, publicReason);
    }

    @Override
    public List<PortraitGenerationJob> findRunning() {
        return jobs.findByStatus(PortraitGenerationStatus.RUNNING).stream().map(this::map).toList();
    }

    private PortraitGenerationJob finish(Long jobId, PortraitGenerationStatus status, String errorMessage) {
        WriteTransactionGuard.requireWriteTransaction();
        PortraitGenerationJobJpaEntity job = jobs.findById(jobId)
            .orElseThrow(() -> new AppException("Trabajo de retratos no encontrado", HttpStatus.NOT_FOUND));
        job.setStatus(status);
        job.setErrorMessage(errorMessage);
        return map(jobs.save(job));
    }

    private PortraitGenerationJob map(PortraitGenerationJobJpaEntity job) {
        return EntityMapperGuard.guard(() -> new PortraitGenerationJob(
            job.getId(), job.getCatalogCharacter().getId(), job.getStatus(), job.getErrorMessage()));
    }
}
