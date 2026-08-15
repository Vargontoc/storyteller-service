package es.vargontoc.storyteller.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CharacterReviewJpaRepository extends JpaRepository<CharacterReviewJpaEntity, Long>  {

    @Query("SELECT cr FROM CharacterReviewJpaEntity cr WHERE cr.character.id = :characterId AND cr.status = 'PENDING'")
    Optional<CharacterReviewJpaEntity> getactiveReviewByCharacterId(@Param("characterId") Long characterId);
}
