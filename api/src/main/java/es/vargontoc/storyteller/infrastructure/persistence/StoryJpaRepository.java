package es.vargontoc.storyteller.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoryJpaRepository extends JpaRepository<StoryJpaEntity, Long> {
    @Modifying
    @Query("UPDATE StoryJpaEntity s SET s.summary = :summary WHERE s.id = :storyId")
    void setSummary(@Param("storyId") Long storyId, @Param("summary") String summary);
}