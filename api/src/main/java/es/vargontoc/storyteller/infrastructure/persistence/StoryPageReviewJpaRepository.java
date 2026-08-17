package es.vargontoc.storyteller.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;


public interface StoryPageReviewJpaRepository extends JpaRepository<StoryPageReviewJpaEntity, Long>  {
    
}
