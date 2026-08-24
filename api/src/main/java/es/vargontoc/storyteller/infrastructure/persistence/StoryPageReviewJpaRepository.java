package es.vargontoc.storyteller.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface StoryPageReviewJpaRepository extends JpaRepository<StoryPageReviewJpaEntity, Long>  {
    @Query("SELECT sr FROM StoryPageReviewJpaEntity sr WHERE sr.page.id = :pageId AND sr.status = 'PENDING'")
    Optional<StoryPageReviewJpaEntity> getActiveStoryReviewByPageId(@Param("pageId") Long pageId);

    @Modifying
    @Query("DELETE FROM StoryPageReviewJpaEntity sr WHERE sr.page.id IN :pageIds")
    void deleteByPageIds(@Param("pageIds") List<Long> pageIds);
}
