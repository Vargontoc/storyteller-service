package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import es.vargontoc.framework.persistence.jpa.transaction.WriteTransactionGuard;
import es.vargontoc.storyteller.application.ports.out.PortraitCandidateAssetStorage;
import es.vargontoc.storyteller.application.ports.out.persistence.PortraitGenerationJobStore;
import es.vargontoc.storyteller.domain.enums.PortraitGenerationStatus;
import es.vargontoc.storyteller.domain.model.PortraitGenerationJob;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterPortraitJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.PortraitCandidateJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.PortraitCandidateJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.PortraitGenerationJobJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.PortraitGenerationJobJpaRepository;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockedStatic;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PortraitSelectionService.class, PortraitSelectionServiceTest.Dependencies.class,
        PortraitSelectionServiceTest.JpaAuditingTestConfiguration.class})
class PortraitSelectionServiceTest {
    @org.springframework.beans.factory.annotation.Autowired private PortraitSelectionService service;
    @org.springframework.beans.factory.annotation.Autowired private CatalogCharacterJpaRepository characters;
    @org.springframework.beans.factory.annotation.Autowired private PortraitCandidateJpaRepository candidates;
    @org.springframework.beans.factory.annotation.Autowired private PortraitGenerationJobJpaRepository jobs;
    @org.springframework.beans.factory.annotation.Autowired private CatalogCharacterPortraitJpaRepository portraits;
    @org.springframework.beans.factory.annotation.Autowired private PortraitGenerationJobStore jobStore;
    @org.springframework.beans.factory.annotation.Autowired private PortraitCandidateAssetStorage assets;

    @Test
    void selectsOneCandidatePreservesItsAssetAndDeletesTheOtherThree() {
        var character = character("Luna");
        var gallery = gallery(character);
        when(jobStore.findRunningByCatalogCharacterId(character.getId())).thenReturn(Optional.empty());

        service.select(character.getId(), gallery.get(1).getId());

        var approved = portraits.findByCatalogCharacterId(character.getId()).orElseThrow();
        assertTrue(approved.getAssetPath().endsWith("2.png"));
        assertFalse(candidates.existsByCatalogCharacterId(character.getId()));
        // La transacción de DataJpaTest se revierte: la limpieza física queda programada solo para afterCommit.
        verify(assets, never()).deleteCandidateAsset("characters/2.png");
    }

    @Test
    void staleOrForeignCandidateDoesNotChangeTheExistingGalleryOrPortrait() {
        var first = character("Luna");
        var second = character("Nora");
        var firstGallery = gallery(first);
        var foreign = gallery(second).getFirst();
        when(jobStore.findRunningByCatalogCharacterId(first.getId())).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> service.select(first.getId(), foreign.getId()));

        assertFalse(portraits.existsByCatalogCharacterId(first.getId()));
        assertTrue(candidates.existsById(firstGallery.getFirst().getId()));
        assertTrue(candidates.existsById(foreign.getId()));
        verify(assets, never()).deleteCandidateAsset(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void runningGenerationRejectsSelectionBeforeChangingCandidates() {
        var character = character("Luna");
        var candidate = gallery(character).getFirst();
        when(jobStore.findRunningByCatalogCharacterId(character.getId()))
                .thenReturn(Optional.of(new PortraitGenerationJob(90L, character.getId(), PortraitGenerationStatus.RUNNING, null)));

        assertThrows(AppException.class, () -> service.select(character.getId(), candidate.getId()));

        assertFalse(portraits.existsByCatalogCharacterId(character.getId()));
        assertTrue(candidates.existsById(candidate.getId()));
        verify(assets, never()).deleteCandidateAsset(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void discardedAssetsAreDeletedOnlyAfterTheDatabaseTransactionCommits() {
        var character = Mockito.mock(CatalogCharacterJpaEntity.class);
        var selected = candidate(2L, "characters/2.png");
        var first = candidate(1L, "characters/1.png");
        var third = candidate(3L, "characters/3.png");
        var fourth = candidate(4L, "characters/4.png");
        var characterRepository = Mockito.mock(CatalogCharacterJpaRepository.class);
        var candidateRepository = Mockito.mock(PortraitCandidateJpaRepository.class);
        var portraitRepository = Mockito.mock(CatalogCharacterPortraitJpaRepository.class);
        var runningJobs = Mockito.mock(PortraitGenerationJobStore.class);
        var assetStorage = Mockito.mock(PortraitCandidateAssetStorage.class);
        when(characterRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(character));
        when(runningJobs.findRunningByCatalogCharacterId(7L)).thenReturn(Optional.empty());
        when(portraitRepository.existsByCatalogCharacterId(7L)).thenReturn(false);
        when(candidateRepository.findByCatalogCharacterId(7L)).thenReturn(List.of(first, selected, third, fourth));
        var unit = new PortraitSelectionService(characterRepository, candidateRepository, portraitRepository,
                runningJobs, assetStorage);

        int existingSynchronizations = org.springframework.transaction.support.TransactionSynchronizationManager
                .getSynchronizations().size();
        try (MockedStatic<WriteTransactionGuard> ignored = Mockito.mockStatic(WriteTransactionGuard.class)) {
            unit.select(7L, 2L);
            verify(assetStorage, never()).deleteCandidateAsset(org.mockito.ArgumentMatchers.anyString());
            org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations()
                    .subList(existingSynchronizations, org.springframework.transaction.support.TransactionSynchronizationManager
                            .getSynchronizations().size())
                    .forEach(org.springframework.transaction.support.TransactionSynchronization::afterCommit);
        }

        verify(assetStorage).deleteCandidateAsset("characters/1.png");
        verify(assetStorage).deleteCandidateAsset("characters/3.png");
        verify(assetStorage).deleteCandidateAsset("characters/4.png");
        verify(assetStorage, never()).deleteCandidateAsset("characters/2.png");
    }

    private PortraitCandidateJpaEntity candidate(Long id, String assetPath) {
        var candidate = Mockito.mock(PortraitCandidateJpaEntity.class);
        when(candidate.getId()).thenReturn(id);
        when(candidate.getAssetPath()).thenReturn(assetPath);
        return candidate;
    }

    private CatalogCharacterJpaEntity character(String name) {
        var character = new CatalogCharacterJpaEntity();
        character.setName(name);
        character.setNarrativeDescription("Narrativa");
        character.setVisualDescription("Visual");
        character.setMetadata(new es.vargontoc.storyteller.domain.model.ActorMetadata("fox", List.of()));
        return characters.saveAndFlush(character);
    }

    private List<PortraitCandidateJpaEntity> gallery(CatalogCharacterJpaEntity character) {
        var job = new PortraitGenerationJobJpaEntity();
        job.setCatalogCharacter(character);
        job.setStatus(PortraitGenerationStatus.COMPLETED);
        job = jobs.saveAndFlush(job);
        var generationJob = job;
        return java.util.stream.IntStream.rangeClosed(1, 4).mapToObj(number -> {
            var candidate = new PortraitCandidateJpaEntity();
            candidate.setCatalogCharacter(character);
            candidate.setGenerationJob(generationJob);
            candidate.setAssetPath("characters/" + number + ".png");
            return candidates.saveAndFlush(candidate);
        }).toList();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Dependencies {
        @Bean PortraitGenerationJobStore portraitGenerationJobStore() { return Mockito.mock(PortraitGenerationJobStore.class); }
        @Bean PortraitCandidateAssetStorage portraitCandidateAssetStorage() { return Mockito.mock(PortraitCandidateAssetStorage.class); }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableJpaAuditing
    static class JpaAuditingTestConfiguration { }
}
