package es.vargontoc.storyteller.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.vargontoc.storyteller.domain.enums.PortraitGenerationStatus;
import es.vargontoc.storyteller.domain.enums.StorySize;
import es.vargontoc.storyteller.domain.enums.StoryStatus;
import es.vargontoc.storyteller.domain.model.ActorMetadata;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
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
@Import(CatalogPortraitPersistenceTest.JpaAuditingTestConfiguration.class)
class CatalogPortraitPersistenceTest {
    @Autowired private CatalogCharacterJpaRepository catalogCharacters;
    @Autowired private CatalogCharacterPortraitJpaRepository portraits;
    @Autowired private PortraitGenerationJobJpaRepository jobs;
    @Autowired private PortraitCandidateJpaRepository candidates;
    @Autowired private CatalogCharacterForkJpaRepository forks;
    @Autowired private EntityManager entityManager;

    @Test
    void persistsPortraitCandidatesJobsAndForkWithoutChangingTheOriginal() {
        CatalogCharacterJpaEntity original = savedCatalogCharacter("Luna", "zorra naranja");
        CatalogCharacterJpaEntity fork = savedCatalogCharacter("Luna revisada", "zorra plateada");

        CatalogCharacterPortraitJpaEntity portrait = new CatalogCharacterPortraitJpaEntity();
        portrait.setCatalogCharacter(original);
        portrait.setAssetPath("portraits/luna-approved.png");
        portraits.saveAndFlush(portrait);

        PortraitGenerationJobJpaEntity job = new PortraitGenerationJobJpaEntity();
        job.setCatalogCharacter(original);
        job.setStatus(PortraitGenerationStatus.RUNNING);
        job = jobs.saveAndFlush(job);
        assertEquals(PortraitGenerationStatus.RUNNING, jobs.findById(job.getId()).orElseThrow().getStatus());

        job.setStatus(PortraitGenerationStatus.COMPLETED);
        jobs.saveAndFlush(job);
        assertEquals(PortraitGenerationStatus.COMPLETED, jobs.findById(job.getId()).orElseThrow().getStatus());
        job.setStatus(PortraitGenerationStatus.FAILED);
        jobs.saveAndFlush(job);
        assertEquals(PortraitGenerationStatus.FAILED, jobs.findById(job.getId()).orElseThrow().getStatus());

        PortraitCandidateJpaEntity first = candidate(original, job, "portraits/luna-1.png");
        candidates.save(first);
        candidates.save(candidate(original, job, "portraits/luna-2.png"));
        candidates.save(candidate(original, job, "portraits/luna-3.png"));
        candidates.save(candidate(original, job, "portraits/luna-4.png"));
        candidates.flush();
        assertEquals(4, candidates.findByCatalogCharacterId(original.getId()).size());
        candidates.deleteById(first.getId());
        candidates.flush();
        assertEquals(3, candidates.findByCatalogCharacterId(original.getId()).size());
        candidates.deleteByCatalogCharacterId(original.getId());
        candidates.flush();
        assertTrue(candidates.findByCatalogCharacterId(original.getId()).isEmpty());

        CatalogCharacterForkJpaEntity relation = new CatalogCharacterForkJpaEntity();
        relation.setOriginalCatalogCharacter(original);
        relation.setForkCatalogCharacter(fork);
        forks.saveAndFlush(relation);
        entityManager.clear();

        CatalogCharacterJpaEntity reloadedOriginal = catalogCharacters.findById(original.getId()).orElseThrow();
        assertEquals("Luna", reloadedOriginal.getName());
        assertEquals("zorra naranja", reloadedOriginal.getVisualDescription());
        assertEquals("portraits/luna-approved.png", portraits.findByCatalogCharacterId(original.getId()).orElseThrow().getAssetPath());
        assertEquals(fork.getId(), forks.findByOriginalCatalogCharacterId(original.getId()).getFirst().getForkCatalogCharacter().getId());
    }

    @Test
    void preservesNullForLegacySnapshotsAndPersistsAnExplicitCatalogReference() {
        CatalogCharacterJpaEntity catalog = savedCatalogCharacter("Luna", "zorra naranja");
        StoryJpaEntity story = savedStory();

        CharacterJpaEntity legacy = character(story, null, "Luna histórica");
        CharacterJpaEntity current = character(story, catalog, "Luna nueva");
        entityManager.persist(legacy);
        entityManager.persist(current);
        entityManager.flush();
        entityManager.clear();

        assertNull(entityManager.find(CharacterJpaEntity.class, legacy.getId()).getCatalogCharacter());
        assertEquals(catalog.getId(), entityManager.find(CharacterJpaEntity.class, current.getId()).getCatalogCharacter().getId());
    }

    private CatalogCharacterJpaEntity savedCatalogCharacter(String name, String visualDescription) {
        CatalogCharacterJpaEntity character = new CatalogCharacterJpaEntity();
        character.setName(name);
        character.setNarrativeDescription("Una protagonista amable");
        character.setVisualDescription(visualDescription);
        character.setMetadata(new ActorMetadata("friendly fox", List.of("orange")));
        return catalogCharacters.saveAndFlush(character);
    }

    private PortraitCandidateJpaEntity candidate(CatalogCharacterJpaEntity character, PortraitGenerationJobJpaEntity job, String path) {
        PortraitCandidateJpaEntity candidate = new PortraitCandidateJpaEntity();
        candidate.setCatalogCharacter(character);
        candidate.setGenerationJob(job);
        candidate.setAssetPath(path);
        return candidate;
    }

    private StoryJpaEntity savedStory() {
        StoryJpaEntity story = new StoryJpaEntity();
        story.setTopic("Bosque");
        story.setTopicDescription("Una aventura amable");
        story.setTitle("La aventura de Luna");
        story.setSynopsis("Luna aprende a ayudar");
        story.setSize(StorySize.S);
        story.setStatus(StoryStatus.SCRIPT_PENDING);
        story.setCreatedAt(LocalDateTime.now());
        entityManager.persist(story);
        return story;
    }

    private CharacterJpaEntity character(StoryJpaEntity story, CatalogCharacterJpaEntity catalog, String name) {
        CharacterJpaEntity character = CharacterJpaEntity.draft(true, name, "zorra naranja", "Una zorra amable", new ActorMetadata("orange fox", List.of("orange")));
        character.setStory(story);
        character.setCatalogCharacter(catalog);
        character.setCreatedAt(LocalDateTime.now());
        return character;
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableJpaAuditing
    static class JpaAuditingTestConfiguration { }
}
