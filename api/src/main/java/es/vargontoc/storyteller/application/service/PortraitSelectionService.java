package es.vargontoc.storyteller.application.service;

import es.vargontoc.framework.persistence.jpa.transaction.WriteTransactionGuard;
import es.vargontoc.storyteller.application.ports.in.character.PortraitSelectionUseCase;
import es.vargontoc.storyteller.application.ports.out.PortraitCandidateAssetStorage;
import es.vargontoc.storyteller.application.ports.out.persistence.PortraitGenerationJobStore;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterPortraitJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterPortraitJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.PortraitCandidateJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.PortraitCandidateJpaRepository;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class PortraitSelectionService implements PortraitSelectionUseCase {
    private final CatalogCharacterJpaRepository characters;
    private final PortraitCandidateJpaRepository candidates;
    private final CatalogCharacterPortraitJpaRepository portraits;
    private final PortraitGenerationJobStore jobs;
    private final PortraitCandidateAssetStorage assets;

    public PortraitSelectionService(CatalogCharacterJpaRepository characters,
            PortraitCandidateJpaRepository candidates, CatalogCharacterPortraitJpaRepository portraits,
            PortraitGenerationJobStore jobs, PortraitCandidateAssetStorage assets) {
        this.characters = characters;
        this.candidates = candidates;
        this.portraits = portraits;
        this.jobs = jobs;
        this.assets = assets;
    }

    @Override
    @Transactional
    public void select(Long catalogCharacterId, Long candidateId) {
        WriteTransactionGuard.requireWriteTransaction();
        CatalogCharacterJpaEntity character = characters.findByIdForUpdate(catalogCharacterId)
                .orElseThrow(() -> new AppException("Catálogo no encontrado", HttpStatus.NOT_FOUND));
        if (jobs.findRunningByCatalogCharacterId(catalogCharacterId).isPresent()) {
            throw new AppException("El personaje tiene una generación de retratos en curso", HttpStatus.CONFLICT);
        }
        if (portraits.existsByCatalogCharacterId(catalogCharacterId)) {
            throw new AppException("El personaje ya tiene un retrato aprobado", HttpStatus.CONFLICT);
        }

        List<PortraitCandidateJpaEntity> gallery = candidates.findByCatalogCharacterId(catalogCharacterId);
        PortraitCandidateJpaEntity selected = gallery.stream()
                .filter(candidate -> candidate.getId().equals(candidateId))
                .findFirst()
                .orElseThrow(() -> new AppException("El candidato de retrato ya no está vigente", HttpStatus.NOT_FOUND));

        CatalogCharacterPortraitJpaEntity portrait = new CatalogCharacterPortraitJpaEntity();
        portrait.setCatalogCharacter(character);
        portrait.setAssetPath(selected.getAssetPath());
        portraits.save(portrait);

        gallery.stream()
                .filter(candidate -> !candidate.getId().equals(candidateId))
                .forEach(this::discard);
        candidates.delete(selected);
    }

    private void discard(PortraitCandidateJpaEntity candidate) {
        candidates.delete(candidate);
        String assetPath = candidate.getAssetPath();
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            assets.deleteCandidateAsset(assetPath);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                assets.deleteCandidateAsset(assetPath);
            }
        });
    }
}
