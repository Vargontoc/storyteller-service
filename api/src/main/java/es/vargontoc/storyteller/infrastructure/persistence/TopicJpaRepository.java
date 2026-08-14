package es.vargontoc.storyteller.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TopicJpaRepository extends JpaRepository<TopicJpaEntity, Long> {

    @Query("SELECT s.topic FROM StoryJpaEntity s WHERE s.id = :storyId")
    Optional<TopicJpaEntity> getTopicByStory(@Param("storyId") Long storyId);
}
