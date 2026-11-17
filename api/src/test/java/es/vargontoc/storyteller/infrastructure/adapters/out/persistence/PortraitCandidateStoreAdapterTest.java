package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.clearInvocations;

import es.vargontoc.storyteller.application.ports.out.PortraitCandidateAssetStorage;
import es.vargontoc.storyteller.application.ports.out.persistence.PortraitCandidateStore;
import es.vargontoc.storyteller.domain.enums.PortraitGenerationStatus;
import es.vargontoc.storyteller.domain.model.ActorMetadata;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterPortraitJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterPortraitJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.PortraitCandidateJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.PortraitGenerationJobJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.PortraitGenerationJobJpaRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PortraitCandidateStoreAdapter.class, PortraitCandidateStoreAdapterTest.JpaAuditingTestConfiguration.class})
class PortraitCandidateStoreAdapterTest {
    @Autowired private PortraitCandidateStore store;
    @Autowired private PortraitCandidateAssetStorage assets;
    @Autowired private CatalogCharacterJpaRepository catalogCharacters;
    @Autowired private PortraitGenerationJobJpaRepository jobs;
    @Autowired private PortraitCandidateJpaRepository candidates;
    @Autowired private CatalogCharacterPortraitJpaRepository approvedPortraits;

    @BeforeEach
    void clearAssetInteractions() {
        clearInvocations(assets);
    }

    @Test
    void storesExactlyFourCandidatesForTheCharacterAndJob() {
        CatalogCharacterJpaEntity character = character("Luna");
        PortraitGenerationJobJpaEntity job = job(character);

        var stored = store.replace(character.getId(), job.getId(), paths("first"));

        assertEquals(4, stored.size());
        assertTrue(stored.stream().allMatch(candidate -> candidate.catalogCharacterId().equals(character.getId())));
        assertTrue(stored.stream().allMatch(candidate -> candidate.generationJobId().equals(job.getId())));
        assertEquals(List.of(), store.findByCatalogCharacterId(999L));
    }

    @Test
    void replacesTheWholeCandidateSetAndDeletesOnlyDiscardedCandidateAssets() {
        CatalogCharacterJpaEntity character = character("Luna");
        PortraitGenerationJobJpaEntity job = job(character);
        store.replace(character.getId(), job.getId(), paths("old"));

        var replacement = store.replace(character.getId(), job.getId(), paths("new"));

        assertEquals(replacement.stream().map(candidate -> candidate.assetPath()).toList(),
            store.findByCatalogCharacterId(character.getId()).stream().map(candidate -> candidate.assetPath()).toList());
        paths("old").forEach(path -> verify(assets).deleteCandidateAsset(path));
    }

    @Test
    void deletingOneCandidateKeepsTheOthersAndNeverDeletesTheApprovedPortrait() {
        CatalogCharacterJpaEntity character = character("Luna");
        PortraitGenerationJobJpaEntity job = job(character);
        var stored = store.replace(character.getId(), job.getId(), paths("candidate"));
        CatalogCharacterPortraitJpaEntity approved = new CatalogCharacterPortraitJpaEntity();
        approved.setCatalogCharacter(character);
        approved.setAssetPath("characters/" + character.getId() + "/approved.png");
        approvedPortraits.saveAndFlush(approved);

        store.delete(character.getId(), stored.getFirst().id());

        assertEquals(3, store.findByCatalogCharacterId(character.getId()).size());
        verify(assets).deleteCandidateAsset(stored.getFirst().assetPath());
        verify(assets, never()).deleteCandidateAsset(approved.getAssetPath());
        assertTrue(approvedPortraits.findByCatalogCharacterId(character.getId()).isPresent());
    }

    @Test
    void deletingAllCandidatesRemovesTheirAssets() {
        CatalogCharacterJpaEntity character = character("Nora");
        PortraitGenerationJobJpaEntity job = job(character);
        store.replace(character.getId(), job.getId(), paths("candidate"));

        store.deleteAll(character.getId());

        assertTrue(store.findByCatalogCharacterId(character.getId()).isEmpty());
        paths("candidate").forEach(path -> verify(assets).deleteCandidateAsset(path));
    }

    private CatalogCharacterJpaEntity character(String name) {
        CatalogCharacterJpaEntity character = new CatalogCharacterJpaEntity();
        character.setName(name);
        character.setNarrativeDescription("Una protagonista amable");
        character.setVisualDescription("Zorra naranja");
        character.setMetadata(new ActorMetadata("orange fox", List.of("orange")));
        return catalogCharacters.saveAndFlush(character);
    }

    private PortraitGenerationJobJpaEntity job(CatalogCharacterJpaEntity character) {
        PortraitGenerationJobJpaEntity job = new PortraitGenerationJobJpaEntity();
        job.setCatalogCharacter(character);
        job.setStatus(PortraitGenerationStatus.COMPLETED);
        return jobs.saveAndFlush(job);
    }

    private List<String> paths(String batch) {
        return List.of(1, 2, 3, 4).stream().map(number -> "characters/1/" + batch + "-" + number + ".png").toList();
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableJpaAuditing
    static class JpaAuditingTestConfiguration {
        @Bean
        PortraitCandidateAssetStorage portraitCandidateAssetStorage() {
            return org.mockito.Mockito.mock(PortraitCandidateAssetStorage.class);
        }
    }
}
