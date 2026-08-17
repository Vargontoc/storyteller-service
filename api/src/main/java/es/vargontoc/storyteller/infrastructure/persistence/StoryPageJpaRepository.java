package es.vargontoc.storyteller.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;



public interface StoryPageJpaRepository extends JpaRepository<StoryPageJpaEntity, Long> {
    
}
