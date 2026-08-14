package es.vargontoc.storyteller.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CharacterJpaRepository extends JpaRepository<CharacterJpaEntity, Long> {

    @Modifying
    @Query("DELETE FROM CharacterJpaEntity c WHERE c.story.id = :storyId")
    void deleteFromStoryId(@Param("storyId") Long storyId);
 }
