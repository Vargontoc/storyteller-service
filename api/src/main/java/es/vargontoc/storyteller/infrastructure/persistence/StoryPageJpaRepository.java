package es.vargontoc.storyteller.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;



public interface StoryPageJpaRepository extends JpaRepository<StoryPageJpaEntity, Long> {
    
    @Modifying
    @Query("DELETE FROM StoryPageJpaEntity p WHERE p.story.id = :storyId AND p.page > :numberPage")
    void deletePages(@Param("storyId") long storyId, @Param("numberPage") int numberPage);

    @Query("SELECT P.id FROM StoryPageJpaEntity p WHERE p.story.id = :storyId AND p.page > :numberPage")
    List<Long> getPagesIdByStory(@Param("storyId") long storyId, @Param("numberPage") int numberPage); 
    
}
