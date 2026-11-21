package es.vargontoc.storyteller.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoryPageVersionJpaRepository extends JpaRepository<StoryPageVersionJpaEntity, Long> {

    List<StoryPageVersionJpaEntity> findByPageIdOrderByIdAsc(Long pageId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE StoryPageVersionJpaEntity v SET v.current = false WHERE v.page.id = :pageId AND v.current = true")
    void clearCurrent(@Param("pageId") Long pageId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE StoryPageVersionJpaEntity v SET v.current = true WHERE v.page.id = :pageId AND v.id = :versionId")
    int markCurrent(@Param("pageId") Long pageId, @Param("versionId") Long versionId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM StoryPageVersionJpaEntity v WHERE v.page.id = :pageId")
    void deleteByPageId(@Param("pageId") Long pageId);
}
