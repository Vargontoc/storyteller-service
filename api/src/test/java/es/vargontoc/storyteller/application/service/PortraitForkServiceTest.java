package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.vargontoc.storyteller.application.ports.out.persistence.CatalogCharacterUsagePort;
import es.vargontoc.storyteller.domain.enums.PortraitGenerationStatus;
import es.vargontoc.storyteller.domain.enums.StorySize;
import es.vargontoc.storyteller.domain.model.ActorMetadata;
import es.vargontoc.storyteller.infrastructure.adapters.out.persistence.CatalogCharacterUsageAdapter;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterForkJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterPortraitJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterPortraitJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.PortraitCandidateJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.PortraitCandidateJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.PortraitGenerationJobJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.PortraitGenerationJobJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.TopicJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.TopicJpaRepository;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PortraitForkService.class, CatalogCharacterUsageAdapter.class,
        PortraitForkServiceTest.JpaAuditingTestConfiguration.class})
class PortraitForkServiceTest {
    @Autowired private PortraitForkService service;
    @Autowired private CatalogCharacterJpaRepository catalogCharacters;
    @Autowired private CatalogCharacterForkJpaRepository forks;
    @Autowired private CatalogCharacterPortraitJpaRepository portraits;
    @Autowired private PortraitCandidateJpaRepository candidates;
    @Autowired private PortraitGenerationJobJpaRepository jobs;
    @Autowired private CharacterJpaRepository snapshots;
    @Autowired private StoryJpaRepository stories;
    @Autowired private TopicJpaRepository topics;
    @Autowired private CatalogCharacterUsagePort usage;

    @Test
    void createsAVisualForkWithoutChangingTheUsedOriginalOrItsAssets() {
        CatalogCharacterJpaEntity original = character("Luna", "zorra naranja");
        createAssets(original);
        CharacterJpaEntity snapshot = snapshot(original, "Luna histórica");
        ActorMetadata revisedMetadata = new ActorMetadata("silver fox", List.of("silver"));

        var fork = service.create(original.getId(), "Zorra plateada", revisedMetadata);

        assertTrue(fork.name().startsWith("Luna"));
        assertEquals("Zorra plateada", fork.visualDescription());
        assertEquals("silver fox", fork.visualDescriptionEn());
        assertFalse(portraits.existsByCatalogCharacterId(fork.id()));
        assertFalse(candidates.existsByCatalogCharacterId(fork.id()));
        assertEquals(1, forks.findByOriginalCatalogCharacterId(original.getId()).size());
        assertEquals("zorra naranja", catalogCharacters.findById(original.getId()).orElseThrow().getVisualDescription());
        assertTrue(portraits.existsByCatalogCharacterId(original.getId()));
        assertTrue(candidates.existsByCatalogCharacterId(original.getId()));
        assertEquals(original.getId(), snapshots.findById(snapshot.getId()).orElseThrow().getCatalogCharacter().getId());
    }

    @Test
    void renamingTheForkDoesNotRenameTheOriginalOrItsStorySnapshot() {
        CatalogCharacterJpaEntity original = character("Luna", "zorra naranja");
        CharacterJpaEntity snapshot = snapshot(original, "Luna histórica");
        var fork = service.create(original.getId(), "Zorra plateada", new ActorMetadata("silver fox", List.of()));

        var renamed = service.rename(fork.id(), "Nora");

        assertEquals("Nora", renamed.name());
        assertEquals("Luna", catalogCharacters.findById(original.getId()).orElseThrow().getName());
        assertEquals("Luna histórica", snapshots.findById(snapshot.getId()).orElseThrow().getName());
    }

    @Test
    void legacySnapshotWithoutTheCatalogForeignKeyDoesNotAuthorizeAForkByName() {
        CatalogCharacterJpaEntity original = character("Luna", "zorra naranja");
        snapshot(null, "Luna");

        assertFalse(usage.isUsedByStorySnapshot(original.getId()));
        assertThrows(AppException.class, () -> service.create(original.getId(), "Zorra plateada",
            new ActorMetadata("silver fox", List.of())));
    }

    private CatalogCharacterJpaEntity character(String name, String visual) {
        CatalogCharacterJpaEntity character = new CatalogCharacterJpaEntity();
        character.setName(name);
        character.setNarrativeDescription("Una protagonista amable");
        character.setVisualDescription(visual);
        character.setMetadata(new ActorMetadata("orange fox", List.of("orange")));
        return catalogCharacters.saveAndFlush(character);
    }

    private void createAssets(CatalogCharacterJpaEntity original) {
        CatalogCharacterPortraitJpaEntity portrait = new CatalogCharacterPortraitJpaEntity();
        portrait.setCatalogCharacter(original);
        portrait.setAssetPath("characters/original-approved.png");
        portraits.saveAndFlush(portrait);
        PortraitGenerationJobJpaEntity job = new PortraitGenerationJobJpaEntity();
        job.setCatalogCharacter(original);
        job.setStatus(PortraitGenerationStatus.COMPLETED);
        jobs.saveAndFlush(job);
        PortraitCandidateJpaEntity candidate = new PortraitCandidateJpaEntity();
        candidate.setCatalogCharacter(original);
        candidate.setGenerationJob(job);
        candidate.setAssetPath("characters/original-candidate.png");
        candidates.saveAndFlush(candidate);
    }

    private CharacterJpaEntity snapshot(CatalogCharacterJpaEntity catalogCharacter, String name) {
        TopicJpaEntity topic = new TopicJpaEntity();
        topic.setName("Amistad");
        topic.setDescription("Una historia amable");
        topics.saveAndFlush(topic);
        StoryJpaEntity story = StoryJpaEntity.draft(topic, StorySize.S, "La aventura", "Una aventura amable");
        stories.saveAndFlush(story);
        CharacterJpaEntity snapshot = CharacterJpaEntity.draft(true, name, "zorra naranja", "Una protagonista amable",
            new ActorMetadata("orange fox", List.of("orange")));
        snapshot.setStory(story);
        snapshot.setCatalogCharacter(catalogCharacter);
        return snapshots.saveAndFlush(snapshot);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableJpaAuditing
    static class JpaAuditingTestConfiguration { }
}
