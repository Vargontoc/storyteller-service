package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.domain.enums.PageStatus;
import es.vargontoc.storyteller.domain.model.SceneComposition;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.infrastructure.mappers.StoryPageMapper;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageReviewJpaRepository;

/** El repositorio JPA se simula con un mapa en memoria. */
class StoryPageRepositoryAdapterTest {

    private final Map<Long, StoryPageJpaEntity> store = new HashMap<>();
    private final AtomicLong ids = new AtomicLong();
    private final StoryPageJpaRepository repository = mock(StoryPageJpaRepository.class);
    private final StoryJpaRepository stories = mock(StoryJpaRepository.class);
    private final StoryJpaEntity story = new StoryJpaEntity();
    private final StoryPageReviewJpaRepository reviews = mock(StoryPageReviewJpaRepository.class);
    private final ResourceStorage storage = mock(ResourceStorage.class);
    private final StoryPageRepositoryAdapter adapter;

    StoryPageRepositoryAdapterTest() {
        story.setId(7L);
        story.setTitle("Luna y el bosque");
        when(stories.findById(7L)).thenReturn(Optional.of(story));
        when(repository.save(any(StoryPageJpaEntity.class))).thenAnswer(call -> {
            StoryPageJpaEntity entity = call.getArgument(0);
            if (entity.getId() == null)
                entity.setId(ids.incrementAndGet());
            store.put(entity.getId(), entity);
            return entity;
        });
        when(repository.findById(any(Long.class))).thenAnswer(call -> Optional.ofNullable(store.get(call.getArgument(0))));
        adapter = new StoryPageRepositoryAdapter(null, repository, reviews,
            stories, new StoryPageMapper(), storage);
    }

    @Test
    void deleteRemovesOnlyThatPage() {
        adapter.delete(5L);

        org.mockito.Mockito.verify(repository).deleteById(5L);
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).deletePages(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void deleteByStoryRemovesAllPagesWithReviewsSummaryAndAssets() {
        when(repository.getPagesIdByStory(7L, -1)).thenReturn(List.of(10L, 11L, 12L));

        int removed = adapter.deleteByStory(7L);

        assertEquals(3, removed);
        org.mockito.InOrder order = org.mockito.Mockito.inOrder(reviews, repository, stories, storage);
        order.verify(reviews).deleteByPageIds(List.of(10L, 11L, 12L));
        order.verify(repository).deletePages(7L, -1);   // story_page_version cae por ON DELETE CASCADE
        order.verify(stories).setSummary(7L, "");
        order.verify(storage).deletePageAssets(7L, 10L, List.of(11L, 12L));
    }

    @Test
    void deleteByStoryWithoutPagesDoesNothing() {
        when(repository.getPagesIdByStory(7L, -1)).thenReturn(List.of());

        assertEquals(0, adapter.deleteByStory(7L));

        org.mockito.Mockito.verifyNoInteractions(reviews, storage);
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).deletePages(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void pendingPageIsReadWithItsBeatAndNoAcceptedContentNorImage() {
        StoryPage created = adapter.createPending(7L, 3, 99L);

        StoryPage read = adapter.getPage(created.getId());

        assertEquals(PageStatus.PENDING, read.getPageStatus());
        assertEquals(3, read.getPage());
        assertEquals(99L, read.getBeatId());
        assertNull(read.getText());
        assertNull(read.getComposition());
        assertNull(read.getImportedImage());
    }

    @Test
    void acceptedPageWithPreparedImageIsReadWithContentAndImage() {
        StoryPage created = adapter.createPending(7L, 1, 10L);
        SceneComposition scene = new SceneComposition("Luna en el claro", List.of(), "claro");
        adapter.accept(created.getId(), "Luna sale.", scene);
        store.get(created.getId()).setImportedImage("img-1");   // dato preparado: la importacion esta fuera de alcance

        StoryPage read = adapter.getPage(created.getId());

        assertEquals(PageStatus.ACCEPTED, read.getPageStatus());
        assertEquals("Luna sale.", read.getText());
        assertEquals(scene, read.getComposition());
        assertEquals("img-1", read.getImportedImage());
    }

    @Test
    void removingTheImageKeepsTheAcceptedContent() {
        StoryPage created = adapter.createPending(7L, 1, 10L);
        SceneComposition scene = new SceneComposition("Luna en el claro", List.of(), "claro");
        adapter.accept(created.getId(), "Luna sale.", scene);
        store.get(created.getId()).setImportedImage("img-1");

        adapter.removeImportedImage(created.getId());

        StoryPage read = adapter.getPage(created.getId());
        assertNull(read.getImportedImage());
        assertEquals(PageStatus.ACCEPTED, read.getPageStatus());
        assertEquals("Luna sale.", read.getText());
        assertEquals(scene, read.getComposition());
    }

    @Test
    void acceptedCoverHasTitleAndSceneButNoNarrativeText() {
        StoryPage cover = adapter.createPending(7L, 0, null);
        SceneComposition scene = new SceneComposition("Portada", List.of(), "bosque");
        adapter.accept(cover.getId(), null, scene);

        StoryPage read = adapter.getPage(cover.getId());

        assertEquals(PageStatus.ACCEPTED, read.getPageStatus());
        assertEquals("Luna y el bosque", read.getCoverText());
        assertNull(read.getText());
        assertNull(read.getBeatId());
        assertEquals(scene, read.getComposition());
    }

    @Test
    void legacyPageIsReadAsBeforeWithoutStatusBeatOrImage() {
        StoryPageJpaEntity legacy = new StoryPageJpaEntity();
        legacy.setId(50L);
        legacy.setStory(story);
        legacy.setPage(2);
        legacy.setText("Texto antiguo");
        legacy.setImageAsset("pages/50.png");
        store.put(50L, legacy);

        StoryPage read = adapter.getPage(50L);

        assertEquals("Texto antiguo", read.getText());
        assertEquals(2, read.getPage());
        assertEquals(7L, read.getStoryId());
        assertNull(read.getPageStatus());
        assertNull(read.getBeatId());
        assertNull(read.getImportedImage());
    }
}
