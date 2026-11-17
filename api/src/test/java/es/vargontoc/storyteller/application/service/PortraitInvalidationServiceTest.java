package es.vargontoc.storyteller.application.service;

import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import es.vargontoc.storyteller.application.ports.out.PortraitCandidateAssetStorage;
import es.vargontoc.storyteller.infrastructure.persistence.*;

class PortraitInvalidationServiceTest {
    @Test
    void removesRowsButDefersPortraitAndCandidateFilesUntilCommit() {
        var portraits=mock(CatalogCharacterPortraitJpaRepository.class); var candidates=mock(PortraitCandidateJpaRepository.class); var assets=mock(PortraitCandidateAssetStorage.class);
        var portrait=mock(CatalogCharacterPortraitJpaEntity.class); when(portrait.getAssetPath()).thenReturn("characters/approved.png");
        var candidate=mock(PortraitCandidateJpaEntity.class); when(candidate.getAssetPath()).thenReturn("characters/candidate.png");
        when(portraits.findByCatalogCharacterId(7L)).thenReturn(Optional.of(portrait)); when(candidates.findByCatalogCharacterId(7L)).thenReturn(List.of(candidate));
        TransactionSynchronizationManager.initSynchronization();
        try {
            new PortraitInvalidationService(portraits,candidates,assets).invalidate(7L);
            verify(portraits).delete(portrait); verify(candidates).delete(candidate); verifyNoInteractions(assets);
            TransactionSynchronizationManager.getSynchronizations().forEach(sync->sync.afterCommit());
            verify(assets).deleteCandidateAsset("characters/approved.png"); verify(assets).deleteCandidateAsset("characters/candidate.png");
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }
}
