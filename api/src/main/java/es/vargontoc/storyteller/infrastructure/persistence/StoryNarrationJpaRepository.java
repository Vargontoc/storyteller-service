package es.vargontoc.storyteller.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryNarrationJpaRepository extends JpaRepository<StoryNarrationJpaEntity, Long> {

    Optional<StoryNarrationJpaEntity> findByStoryId(Long storyId);
}
