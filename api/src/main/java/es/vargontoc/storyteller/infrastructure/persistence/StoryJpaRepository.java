package es.vargontoc.storyteller.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryJpaRepository extends JpaRepository<StoryJpaEntity, Long> { }