package es.vargontoc.storyteller.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;



public interface StoryPageJpaRepository extends JpaRepository<StoryPageJpaEntity, Long> {
    
    @Modifying
    @Query("DELETE FROM StoryPageJpaEntity p WHERE p.story.id = :storyId AND p.page > :numberPage")
    void deletePages(@Param("storyId") long storyId, @Param("numberPage") int numberPage);

    @Query("SELECT p.id FROM StoryPageJpaEntity p WHERE p.story.id = :storyId AND p.page > :numberPage")
    List<Long> getPagesIdByStory(@Param("storyId") long storyId, @Param("numberPage") int numberPage);

    @Modifying
    @Query("UPDATE StoryPageJpaEntity p SET p.audioAsset = :asset WHERE p.story.id = :storyId AND p.page = :numberPage")
    void setAudio(@Param("storyId") long storyId, @Param("numberPage") int numberPage, @Param("asset") String asset);
    
    @Query("SELECT c FROM CharacterJpaEntity c WHERE c.story.id = :storyId AND c.main = true")
    Optional<CharacterJpaEntity> getMainCharacter(@Param("storyId") Long storyId);

    @Query("SELECT c FROM CharacterJpaEntity c WHERE c.story.id = :storyId AND c.name IN :names")
    List<CharacterJpaEntity> getCharactersFromPage(@Param("storyId") Long storyId, List<String> names);
}
