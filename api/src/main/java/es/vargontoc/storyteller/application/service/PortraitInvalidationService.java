package es.vargontoc.storyteller.application.service;

import es.vargontoc.storyteller.application.ports.out.PortraitCandidateAssetStorage;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterPortraitJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.PortraitCandidateJpaRepository;
import java.util.ArrayList;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class PortraitInvalidationService {
    private final CatalogCharacterPortraitJpaRepository portraits;
    private final PortraitCandidateJpaRepository candidates;
    private final PortraitCandidateAssetStorage assets;

    public PortraitInvalidationService(CatalogCharacterPortraitJpaRepository portraits,
            PortraitCandidateJpaRepository candidates, PortraitCandidateAssetStorage assets) {
        this.portraits = portraits;
        this.candidates = candidates;
        this.assets = assets;
    }

    public void invalidate(Long catalogCharacterId) {
        var paths = new ArrayList<String>();
        portraits.findByCatalogCharacterId(catalogCharacterId).ifPresent(portrait -> {
            paths.add(portrait.getAssetPath());
            portraits.delete(portrait);
        });
        candidates.findByCatalogCharacterId(catalogCharacterId).forEach(candidate -> {
            paths.add(candidate.getAssetPath());
            candidates.delete(candidate);
        });
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    paths.forEach(assets::deleteCandidateAsset);
                }
            });
        }
    }
}
