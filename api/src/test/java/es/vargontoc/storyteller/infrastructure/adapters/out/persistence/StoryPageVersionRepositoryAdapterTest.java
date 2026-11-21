package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import es.vargontoc.storyteller.domain.enums.PageLengthWarning;
import es.vargontoc.storyteller.domain.model.PageVersion;
import es.vargontoc.storyteller.domain.model.SceneComposition;
import es.vargontoc.storyteller.infrastructure.mappers.PageVersionMapper;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageVersionJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageVersionJpaRepository;

/** El repositorio JPA se simula con una lista en memoria que imita sus consultas. */
class StoryPageVersionRepositoryAdapterTest {

    private final List<StoryPageVersionJpaEntity> store = new ArrayList<>();
    private final AtomicLong ids = new AtomicLong();
    private final StoryPageVersionJpaRepository repository = mock(StoryPageVersionJpaRepository.class);
    private final StoryPageJpaRepository pages = mock(StoryPageJpaRepository.class);
    private final StoryPageVersionRepositoryAdapter adapter =
        new StoryPageVersionRepositoryAdapter(repository, pages, new PageVersionMapper());

    StoryPageVersionRepositoryAdapterTest() {
        for (long pageId : new long[] { 1L, 2L }) {
            StoryPageJpaEntity page = new StoryPageJpaEntity();
            page.setId(pageId);
            when(pages.findById(pageId)).thenReturn(Optional.of(page));
        }
        when(repository.save(any(StoryPageVersionJpaEntity.class))).thenAnswer(call -> {
            StoryPageVersionJpaEntity entity = call.getArgument(0);
            entity.setId(ids.incrementAndGet());
            store.add(entity);
            return entity;
        });
        when(repository.findByPageIdOrderByIdAsc(anyLong())).thenAnswer(call -> store.stream()
            .filter(v -> v.getPage().getId().equals(call.getArgument(0)))
            .sorted((a, b) -> a.getId().compareTo(b.getId()))
            .toList());
        org.mockito.Mockito.doAnswer(call -> {
            store.stream().filter(v -> v.getPage().getId().equals(call.getArgument(0))).forEach(v -> v.setCurrent(false));
            return null;
        }).when(repository).clearCurrent(anyLong());
        when(repository.markCurrent(anyLong(), anyLong())).thenAnswer(call -> {
            var found = store.stream()
                .filter(v -> v.getPage().getId().equals(call.getArgument(0)) && v.getId().equals(call.getArgument(1)))
                .toList();
            found.forEach(v -> v.setCurrent(true));
            return found.size();
        });
        org.mockito.Mockito.doAnswer(call -> {
            store.removeIf(v -> v.getPage().getId().equals(call.getArgument(0)));
            return null;
        }).when(repository).deleteByPageId(anyLong());
    }

    @Test
    void firstCurrentVersionIsTheOnlyOneOfThePage() {
        adapter.add(1L, version("Luna sale.", PageLengthWarning.NONE, true));

        var history = adapter.findByPage(1L);

        assertEquals(1, history.size());
        assertTrue(history.get(0).isCurrent());
    }

    @Test
    void secondCurrentVersionReplacesTheFirstAsCurrent() {
        adapter.add(1L, version("uno", PageLengthWarning.NONE, true));
        adapter.add(1L, version("dos", PageLengthWarning.NONE, true));

        var history = adapter.findByPage(1L);

        assertEquals(List.of("uno", "dos"), history.stream().map(PageVersion::getText).toList());
        assertEquals(List.of(false, true), history.stream().map(PageVersion::isCurrent).toList());
    }

    @Test
    void settingCurrentKeepsAllVersionsAndLeavesOnlyOneCurrent() {
        adapter.add(1L, version("uno", PageLengthWarning.NONE, false));
        adapter.add(1L, version("dos", PageLengthWarning.NONE, false));
        adapter.add(1L, version("tres", PageLengthWarning.NONE, true));
        Long first = adapter.findByPage(1L).get(0).getId();

        adapter.setCurrent(1L, first);

        var history = adapter.findByPage(1L);
        assertEquals(3, history.size());
        assertEquals(List.of(true, false, false), history.stream().map(PageVersion::isCurrent).toList());
    }

    @Test
    void settingCurrentWithAVersionOfAnotherPageIsRejected() {
        adapter.add(1L, version("uno", PageLengthWarning.NONE, true));
        adapter.add(2L, version("otra", PageLengthWarning.NONE, true));
        Long otherPageVersion = adapter.findByPage(2L).get(0).getId();

        assertThrows(IllegalArgumentException.class, () -> adapter.setCurrent(1L, otherPageVersion));
    }

    @Test
    void everyWarningIsReadBackUnchanged() {
        for (PageLengthWarning warning : PageLengthWarning.values())
            adapter.add(1L, version("texto", warning, false));

        var warnings = adapter.findByPage(1L).stream().map(PageVersion::getWarning).toList();

        assertEquals(List.of(PageLengthWarning.values()), warnings);
    }

    @Test
    void deletingAPageHistoryLeavesOtherPagesUntouched() {
        adapter.add(1L, version("uno", PageLengthWarning.NONE, true));
        adapter.add(1L, version("dos", PageLengthWarning.NONE, true));
        adapter.add(2L, version("otra", PageLengthWarning.NONE, true));

        adapter.deleteByPage(1L);

        assertTrue(adapter.findByPage(1L).isEmpty());
        assertEquals(1, adapter.findByPage(2L).size());
        assertEquals("otra", adapter.findByPage(2L).get(0).getText());
    }

    @Test
    void elevenVersionsAreStoredAndAnyCanBecomeCurrent() {
        for (int i = 1; i <= 11; i++)
            adapter.add(1L, version("v" + i, PageLengthWarning.NONE, i == 11));
        assertEquals(11, adapter.findByPage(1L).size());

        Long sixth = adapter.findByPage(1L).get(5).getId();
        adapter.setCurrent(1L, sixth);

        var current = adapter.findByPage(1L).stream().filter(PageVersion::isCurrent).toList();
        assertEquals(1, current.size());
        assertEquals("v6", current.get(0).getText());
    }

    @Test
    void coverVersionHasSceneAndNoText() {
        var scene = new SceneComposition("bosque", List.of(), "claro");
        adapter.add(1L, new PageVersion(null, scene, PageLengthWarning.NONE, true));

        var cover = adapter.findByPage(1L).get(0);

        assertNull(cover.getText());
        assertEquals(scene, cover.getScene());
    }

    private PageVersion version(String text, PageLengthWarning warning, boolean current) {
        return new PageVersion(text, new SceneComposition("escena", List.of(), "fondo"), warning, current);
    }
}
