package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.vargontoc.storyteller.domain.enums.PortraitGenerationStatus;
import es.vargontoc.storyteller.domain.model.ActorMetadata;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterPortraitJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterPortraitJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.PortraitCandidateJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.PortraitCandidateJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.PortraitGenerationJobJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.PortraitGenerationJobJpaRepository;
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
@Import({PortraitConditionAdapter.class, PortraitConditionAdapterTest.JpaAuditingTestConfiguration.class})
class PortraitConditionAdapterTest {
    @Autowired private PortraitConditionAdapter conditions;
    @Autowired private CatalogCharacterJpaRepository catalogCharacters;
    @Autowired private CatalogCharacterPortraitJpaRepository approvedPortraits;
    @Autowired private PortraitGenerationJobJpaRepository jobs;
    @Autowired private PortraitCandidateJpaRepository candidates;

    @Test
    void reportsNoPortraitOrGeneratedImagesWhenTheCatalogHasNeither() {
        CatalogCharacterJpaEntity character = character("Luna");

        assertFalse(conditions.hasApprovedPortrait(character.getId()));
        assertFalse(conditions.hasGeneratedImages(character.getId()));
        assertFalse(conditions.hasApprovedPortrait(null));
        assertFalse(conditions.hasGeneratedImages(null));
    }

    @Test
    void reportsAnApprovedPortraitAsAnImageOfItsOwnCatalogCharacter() {
        CatalogCharacterJpaEntity character = character("Luna");
        CatalogCharacterPortraitJpaEntity portrait = new CatalogCharacterPortraitJpaEntity();
        portrait.setCatalogCharacter(character);
        portrait.setAssetPath("characters/approved.png");
        approvedPortraits.saveAndFlush(portrait);

        assertTrue(conditions.hasApprovedPortrait(character.getId()));
        assertTrue(conditions.hasGeneratedImages(character.getId()));
    }

    @Test
    void reportsCandidateImagesWithoutTreatingThemAsApproved() {
        CatalogCharacterJpaEntity character = character("Nora");
        PortraitGenerationJobJpaEntity job = new PortraitGenerationJobJpaEntity();
        job.setCatalogCharacter(character);
        job.setStatus(PortraitGenerationStatus.COMPLETED);
        jobs.saveAndFlush(job);
        PortraitCandidateJpaEntity candidate = new PortraitCandidateJpaEntity();
        candidate.setCatalogCharacter(character);
        candidate.setGenerationJob(job);
        candidate.setAssetPath("characters/candidate.png");
        candidates.saveAndFlush(candidate);

        assertFalse(conditions.hasApprovedPortrait(character.getId()));
        assertTrue(conditions.hasGeneratedImages(character.getId()));
    }

    private CatalogCharacterJpaEntity character(String name) {
        CatalogCharacterJpaEntity character = new CatalogCharacterJpaEntity();
        character.setName(name);
        character.setNarrativeDescription("Una protagonista amable");
        character.setVisualDescription("Zorra naranja");
        character.setMetadata(new ActorMetadata("orange fox", List.of("orange")));
        return catalogCharacters.saveAndFlush(character);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableJpaAuditing
    static class JpaAuditingTestConfiguration { }
}
