package es.vargontoc.storyteller.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryOutlineJpaRepository extends JpaRepository<StoryOutlineJpaEntity, Long> {
    Optional<StoryOutlineJpaEntity> findByStoryId(Long storyId);
}
