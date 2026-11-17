package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.vargontoc.storyteller.application.ports.out.persistence.PortraitGenerationJobStore;
import es.vargontoc.storyteller.domain.enums.PortraitGenerationStatus;
import es.vargontoc.storyteller.domain.model.ActorMetadata;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.PortraitGenerationJobJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.PortraitGenerationJobJpaRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PortraitGenerationJobStoreAdapter.class, PortraitGenerationJobStoreAdapterTest.JpaAuditingTestConfiguration.class})
class PortraitGenerationJobStoreAdapterTest {
    @Autowired private PortraitGenerationJobStore store;
    @Autowired private CatalogCharacterJpaRepository catalogCharacters;
    @Autowired private PortraitGenerationJobJpaRepository jobs;

    @Test
    void startsAndFindsTheRunningJobForItsCatalogCharacter() {
        CatalogCharacterJpaEntity character = character("Luna");

        var job = store.start(character.getId());

        assertEquals(character.getId(), job.catalogCharacterId());
        assertEquals(PortraitGenerationStatus.RUNNING, job.status());
        assertEquals(job, store.findRunningByCatalogCharacterId(character.getId()).orElseThrow());
        assertTrue(store.findRunningByCatalogCharacterId(999L).isEmpty());
    }

    @Test
    void completesAndFailsJobsWithoutLeavingThemInTheRunningList() {
        var completed = store.start(character("Luna").getId());
        var failed = store.start(character("Nora").getId());

        assertEquals(PortraitGenerationStatus.COMPLETED, store.complete(completed.id()).status());
        assertEquals(PortraitGenerationStatus.FAILED, store.fail(failed.id(), "ComfyUI no está disponible.").status());
        assertEquals("ComfyUI no está disponible.", jobs.findById(failed.id()).orElseThrow().getErrorMessage());
        assertTrue(store.findRunning().stream()
            .noneMatch(job -> job.id().equals(completed.id()) || job.id().equals(failed.id())));
    }

    @Test
    void listsOnlyRunningJobsAndTheDatabasePreventsTwoForTheSameCharacter() {
        Set<Long> runningBefore = store.findRunning().stream().map(job -> job.id())
            .collect(java.util.stream.Collectors.toSet());
        CatalogCharacterJpaEntity luna = character("Luna");
        CatalogCharacterJpaEntity nora = character("Nora");
        var running = store.start(luna.getId());
        var completed = store.start(nora.getId());
        store.complete(completed.id());

        Set<Long> runningAfter = store.findRunning().stream().map(job -> job.id())
            .collect(java.util.stream.Collectors.toSet());
        assertEquals(runningBefore.size() + 1, runningAfter.size());
        assertTrue(runningAfter.containsAll(runningBefore));
        assertTrue(runningAfter.contains(running.id()));
        assertFalse(store.findRunningByCatalogCharacterId(nora.getId()).isPresent());
        assertThrows(DataIntegrityViolationException.class, () -> jobs.saveAndFlush(runningJob(luna)));
    }

    private CatalogCharacterJpaEntity character(String name) {
        CatalogCharacterJpaEntity character = new CatalogCharacterJpaEntity();
        character.setName(name);
        character.setNarrativeDescription("Una protagonista amable");
        character.setVisualDescription("Zorra naranja");
        character.setMetadata(new ActorMetadata("orange fox", List.of("orange")));
        return catalogCharacters.saveAndFlush(character);
    }

    private PortraitGenerationJobJpaEntity runningJob(CatalogCharacterJpaEntity character) {
        PortraitGenerationJobJpaEntity job = new PortraitGenerationJobJpaEntity();
        job.setCatalogCharacter(character);
        job.setStatus(PortraitGenerationStatus.RUNNING);
        return job;
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableJpaAuditing
    static class JpaAuditingTestConfiguration { }
}
