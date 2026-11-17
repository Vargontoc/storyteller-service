package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import es.vargontoc.framework.persistence.jpa.mapping.EntityMapperGuard;
import es.vargontoc.framework.persistence.jpa.transaction.WriteTransactionGuard;
import es.vargontoc.storyteller.application.ports.out.PortraitCandidateAssetStorage;
import es.vargontoc.storyteller.application.ports.out.persistence.PortraitCandidateStore;
import es.vargontoc.storyteller.domain.model.PortraitCandidate;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.PortraitCandidateJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.PortraitCandidateJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.PortraitGenerationJobJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.PortraitGenerationJobJpaRepository;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PortraitCandidateStoreAdapter implements PortraitCandidateStore {
    private static final int CANDIDATE_BATCH_SIZE = 4;

    private final CatalogCharacterJpaRepository catalogCharacters;
    private final PortraitGenerationJobJpaRepository jobs;
    private final PortraitCandidateJpaRepository candidates;
    private final PortraitCandidateAssetStorage assets;

    public PortraitCandidateStoreAdapter(CatalogCharacterJpaRepository catalogCharacters,
            PortraitGenerationJobJpaRepository jobs, PortraitCandidateJpaRepository candidates,
            PortraitCandidateAssetStorage assets) {
        this.catalogCharacters = catalogCharacters;
        this.jobs = jobs;
        this.candidates = candidates;
        this.assets = assets;
    }

    @Override
    @Transactional
    public List<PortraitCandidate> replace(Long catalogCharacterId, Long generationJobId, List<String> assetPaths) {
        WriteTransactionGuard.requireWriteTransaction();
        if (assetPaths == null || assetPaths.size() != CANDIDATE_BATCH_SIZE
                || assetPaths.stream().anyMatch(path -> path == null || path.isBlank())) {
            throw new AppException("Se requieren exactamente cuatro retratos", HttpStatus.BAD_REQUEST);
        }
        CatalogCharacterJpaEntity character = catalogCharacters.findByIdForUpdate(catalogCharacterId)
            .orElseThrow(() -> new AppException("Catálogo no encontrado", HttpStatus.NOT_FOUND));
        PortraitGenerationJobJpaEntity job = jobs.findById(generationJobId)
            .orElseThrow(() -> new AppException("Trabajo de retratos no encontrado", HttpStatus.NOT_FOUND));
        if (!job.getCatalogCharacter().getId().equals(character.getId())) {
            throw new AppException("El trabajo no pertenece al personaje", HttpStatus.CONFLICT);
        }

        deleteEntities(candidates.findByCatalogCharacterId(catalogCharacterId));
        List<PortraitCandidateJpaEntity> saved = assetPaths.stream()
            .map(path -> candidate(character, job, path))
            .map(candidates::save)
            .toList();
        return saved.stream().map(this::map).toList();
    }

    @Override
    public List<PortraitCandidate> findByCatalogCharacterId(Long catalogCharacterId) {
        return candidates.findByCatalogCharacterId(catalogCharacterId).stream().map(this::map).toList();
    }

    @Override
    @Transactional
    public void delete(Long catalogCharacterId, Long candidateId) {
        WriteTransactionGuard.requireWriteTransaction();
        PortraitCandidateJpaEntity candidate = candidates.findById(candidateId)
            .filter(value -> value.getCatalogCharacter().getId().equals(catalogCharacterId))
            .orElseThrow(() -> new AppException("Candidato de retrato no encontrado", HttpStatus.NOT_FOUND));
        deleteEntities(List.of(candidate));
    }

    @Override
    @Transactional
    public void deleteAll(Long catalogCharacterId) {
        WriteTransactionGuard.requireWriteTransaction();
        deleteEntities(candidates.findByCatalogCharacterId(catalogCharacterId));
    }

    @Override
    @Transactional
    public void deleteByGenerationJobId(Long generationJobId) {
        WriteTransactionGuard.requireWriteTransaction();
        deleteEntities(candidates.findByGenerationJobId(generationJobId));
    }

    private void deleteEntities(List<PortraitCandidateJpaEntity> discarded) {
        for (PortraitCandidateJpaEntity candidate : discarded) {
            candidates.delete(candidate);
            assets.deleteCandidateAsset(candidate.getAssetPath());
        }
    }

    private PortraitCandidateJpaEntity candidate(CatalogCharacterJpaEntity character,
            PortraitGenerationJobJpaEntity job, String assetPath) {
        PortraitCandidateJpaEntity candidate = new PortraitCandidateJpaEntity();
        candidate.setCatalogCharacter(character);
        candidate.setGenerationJob(job);
        candidate.setAssetPath(assetPath);
        return candidate;
    }

    private PortraitCandidate map(PortraitCandidateJpaEntity candidate) {
        return EntityMapperGuard.guard(() -> new PortraitCandidate(candidate.getId(),
            candidate.getCatalogCharacter().getId(), candidate.getGenerationJob().getId(), candidate.getAssetPath()));
    }
}
