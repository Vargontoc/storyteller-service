package es.vargontoc.storyteller.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface StoryPageReviewJpaRepository extends JpaRepository<StoryPageReviewJpaEntity, Long>  {
    @Query("SELECT sr FROM StoryPageReviewJpaEntity sr WHERE sr.page.id = :pageId AND sr.status = 'PENDING'")
    Optional<StoryPageReviewJpaEntity> getActiveStoryReviewByPageId(@Param("pageId") Long pageId);
}
