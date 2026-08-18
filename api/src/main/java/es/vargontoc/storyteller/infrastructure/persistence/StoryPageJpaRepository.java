package es.vargontoc.storyteller.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;



public interface StoryPageJpaRepository extends JpaRepository<StoryPageJpaEntity, Long> {
    
    @Modifying
    @Query("DELETE FROM StoryPageJpaEntity p WHERE p.story.id = :storyId AND p.page > :numberPage")
    void deletePages(@Param("storyId") long storyId, int numberPage);
    
}
