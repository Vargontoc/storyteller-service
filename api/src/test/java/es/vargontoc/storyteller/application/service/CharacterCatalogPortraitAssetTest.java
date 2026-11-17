package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import es.vargontoc.storyteller.application.ports.in.character.PortraitGenerationUseCase;
import es.vargontoc.storyteller.application.ports.in.character.PortraitSelectionUseCase;
import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.application.ports.out.persistence.CatalogCharacterPendingReviewReader;
import es.vargontoc.storyteller.application.ports.out.persistence.CatalogCharacterRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.PortraitCandidateStore;
import es.vargontoc.storyteller.application.ports.out.external.CharacterAgent;
import es.vargontoc.storyteller.domain.model.PortraitCandidate;
import es.vargontoc.storyteller.infrastructure.validations.CatalogCharacterAgentResultValidator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import es.vargontoc.storyteller.domain.model.CatalogCharacter;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterPortraitJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterPortraitJpaRepository;

class CharacterCatalogPortraitAssetTest {
    @Test
    void approvedPortraitIsResolvedByCatalogIdWithoutAcceptingAClientPath() {
        var repository = mock(CatalogCharacterRepository.class);
        var portraits = mock(CatalogCharacterPortraitJpaRepository.class);
        var storage = mock(ResourceStorage.class);
        var portrait = mock(CatalogCharacterPortraitJpaEntity.class);
        when(repository.findById(7L)).thenReturn(new CatalogCharacter(7L, "Luna", "curiosa", "zorra", "fox", List.of()));
        when(portrait.getAssetPath()).thenReturn("characters/7/approved.png");
        when(portraits.findByCatalogCharacterId(7L)).thenReturn(Optional.of(portrait));
        when(storage.getResource("characters/7/approved.png")).thenReturn(new byte[] { 1, 2 });
        var service = service(repository, mock(PortraitCandidateStore.class), storage, portraits);
        assertArrayEquals(new byte[] { 1, 2 }, service.approvedPortraitAsset(7L));
        verifyNoInteractions(mock(PortraitCandidateStore.class));
    }

    @Test
    void missingApprovedPortraitDoesNotReadStorage() {
        var repository = mock(CatalogCharacterRepository.class);
        var portraits = mock(CatalogCharacterPortraitJpaRepository.class);
        var storage = mock(ResourceStorage.class);
        when(repository.findById(7L)).thenReturn(new CatalogCharacter(7L, "Luna", "curiosa", "zorra", "fox", List.of()));
        when(portraits.findByCatalogCharacterId(7L)).thenReturn(Optional.empty());
        assertThrows(es.vargontoc.storyteller.shared.exceptions.AppException.class,
                () -> service(repository, mock(PortraitCandidateStore.class), storage, portraits).approvedPortraitAsset(7L));
        verifyNoInteractions(storage);
    }
    @Test
    void foreignCandidateAssetIsRejectedWithoutReadingItsPath() {
        var repository = mock(CatalogCharacterRepository.class);
        var candidates = mock(PortraitCandidateStore.class);
        var storage = mock(ResourceStorage.class);
        when(candidates.findByCatalogCharacterId(7L)).thenReturn(List.of(new PortraitCandidate(11L, 8L, 4L, "characters/8.png")));
        var service = service(repository, candidates, storage, mock(CatalogCharacterPortraitJpaRepository.class));

        assertThrows(es.vargontoc.storyteller.shared.exceptions.AppException.class, () -> service.candidateAsset(7L, 11L));
        verifyNoInteractions(storage);
    }

    private CharacterCatalogService service(CatalogCharacterRepository repository, PortraitCandidateStore candidates,
            ResourceStorage storage, CatalogCharacterPortraitJpaRepository portraits) {
        return new CharacterCatalogService(mock(CharacterAgent.class), repository,
                new CatalogCharacterAgentResultValidator(), mock(CatalogCharacterPendingReviewReader.class), Optional.empty(),
                mock(PortraitLockService.class), mock(PortraitGenerationUseCase.class), mock(PortraitSelectionUseCase.class),
                candidates, storage, Optional.of(portraits));
    }
}
