package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.domain.model.PageSummary;
import es.vargontoc.storyteller.domain.model.SceneComposition;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageJpaEntity;

class StorytellerServicePageSummaryTest {

    private StoryJpaRepository repository;
    private StorytellerService service;
    private StoryJpaEntity story;

    @BeforeEach
    void setUp() {
        repository = mock(StoryJpaRepository.class);
        service = new StorytellerService(repository, mock(ResourceStorage.class), mock(StoryRepository.class));
        story = new StoryJpaEntity();
        story.setTitle("Titulo");
        when(repository.findById(1L)).thenReturn(Optional.of(story));
    }

    private StoryPageJpaEntity page(long id, int number, String text, SceneComposition scene) {
        StoryPageJpaEntity p = new StoryPageJpaEntity();
        p.setId(id);
        p.setStory(story);
        p.setPage(number);
        p.setText(text);
        p.setSceneComposition(scene);
        return p;
    }

    @Test
    void pendingPagesWithoutSceneAndTextAreListed() {
        story.setPages(List.of(
            page(10, 0, null, null),
            page(11, 1, null, null)));

        List<PageSummary> pages = service.getPages(1L);

        assertEquals(2, pages.size());
        assertEquals("Titulo", pages.get(0).text());
        assertNull(pages.get(0).scene());
        assertNull(pages.get(1).text());
        assertNull(pages.get(1).scene());
    }

    @Test
    void acceptedPageKeepsTextAndScene() {
        story.setPages(List.of(
            page(11, 1, "Texto uno.", new SceneComposition("Escena uno", List.of(), "bosque"))));

        PageSummary summary = service.getPage(1L, 11L);

        assertEquals("Texto uno.", summary.text());
        assertEquals("Escena uno", summary.scene());
    }
}
