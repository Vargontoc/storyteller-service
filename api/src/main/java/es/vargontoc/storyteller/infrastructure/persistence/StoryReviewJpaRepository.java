package es.vargontoc.storyteller.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoryReviewJpaRepository extends JpaRepository<StoryReviewJpaEntity, Long>  { 

    @Query("SELECT sr FROM StoryReviewJpaEntity sr WHERE sr.story.id = :storyId AND sr.status = 'PENDING'")
    Optional<StoryReviewJpaEntity> getActiveStoryReviewByStoryId(@Param("storyId") Long storyId);

}