package es.vargontoc.storyteller.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicJpaRepository extends JpaRepository<TopicJpaEntity, Long> {}
