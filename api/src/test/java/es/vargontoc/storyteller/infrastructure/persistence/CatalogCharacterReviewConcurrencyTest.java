package es.vargontoc.storyteller.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.dao.DataIntegrityViolationException;
import es.vargontoc.storyteller.domain.enums.CharacterReviewTarget;
import es.vargontoc.storyteller.domain.enums.RevisionStatus;
import es.vargontoc.storyteller.domain.model.ActorMetadata;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CatalogCharacterReviewConcurrencyTest.JpaAuditingTestConfiguration.class)
class CatalogCharacterReviewConcurrencyTest {
    @Autowired private CatalogCharacterJpaRepository characters;
    @Autowired private CatalogCharacterReviewJpaRepository reviews;

    @Test
    void databaseAllowsAtMostOnePendingReviewForTheSameCatalogCharacter() {
        CatalogCharacterJpaEntity character = new CatalogCharacterJpaEntity();
        character.setName("Luna");
        character.setNarrativeDescription("Una zorra amable");
        character.setVisualDescription("Zorra naranja");
        character.setMetadata(new ActorMetadata("orange fox", List.of("orange")));
        characters.saveAndFlush(character);

        reviews.saveAndFlush(pendingReview(character));
        assertThrows(DataIntegrityViolationException.class, () -> reviews.saveAndFlush(pendingReview(character)));
    }

    private CatalogCharacterReviewJpaEntity pendingReview(CatalogCharacterJpaEntity character) {
        CatalogCharacterReviewJpaEntity review = new CatalogCharacterReviewJpaEntity();
        review.setCatalogCharacter(character);
        review.setStatus(RevisionStatus.PENDING);
        review.setTarget(CharacterReviewTarget.NARRATIVE);
        review.setHint("Hazla más alegre");
        review.setCandidateName("Luna");
        review.setCandidateNarrative("Una zorra muy alegre");
        review.setCandidateVisual("Zorra naranja");
        review.setMetadata(new ActorMetadata("orange fox", List.of("orange")));
        return review;
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableJpaAuditing
    static class JpaAuditingTestConfiguration { }
}
