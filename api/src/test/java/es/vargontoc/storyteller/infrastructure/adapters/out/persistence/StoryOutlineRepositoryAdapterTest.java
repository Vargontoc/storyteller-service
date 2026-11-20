package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import es.vargontoc.storyteller.domain.model.EpisodicCastMember;
import es.vargontoc.storyteller.domain.model.StoryBeat;
import es.vargontoc.storyteller.domain.model.StoryOutline;
import es.vargontoc.storyteller.domain.model.StoryScriptVersion;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryOutlineJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryOutlineJpaRepository;

class StoryOutlineRepositoryAdapterTest {

    private final StoryJpaRepository stories = mock(StoryJpaRepository.class);
    private final StoryOutlineJpaRepository outlines = mock(StoryOutlineJpaRepository.class);
    private final StoryOutlineRepositoryAdapter adapter = new StoryOutlineRepositoryAdapter(stories, outlines);

    @Test
    void savesBeatsInPageOrderWithFiveFieldsAndMarksTheStoryAsNew() {
        StoryJpaEntity story = story(5L, "El paseo", "Luna sale");
        when(stories.findById(5L)).thenReturn(Optional.of(story));
        when(outlines.findByStoryId(5L)).thenReturn(Optional.empty());

        adapter.save(5L, new StoryOutline(List.of(new EpisodicCastMember("Bruno", "amigo")), List.of(beat("uno"), beat("dos"))));

        var captor = ArgumentCaptor.forClass(StoryOutlineJpaEntity.class);
        verify(outlines).save(captor.capture());
        var saved = captor.getValue();
        assertEquals(List.of(1, 2), saved.getBeats().stream().map(b -> b.getPageNumber()).toList());
        var first = saved.getBeats().get(0);
        assertEquals("uno", first.getEvent());
        assertEquals("funcion", first.getNarrativeFunction());
        assertEquals("Luna\nBruno", first.getParticipants());
        assertEquals("alegre", first.getTone());
        assertEquals("bosque", first.getVisualNote());
        assertEquals(Integer.valueOf(StoryScriptVersion.OUTLINE), story.getScriptVersion());
    }

    @Test
    void readsTheScriptBackWithBeatsAndCastAndWithoutCatalogEntries() {
        StoryJpaEntity story = story(5L, "El paseo", "Luna sale");
        when(stories.findById(5L)).thenReturn(Optional.of(story));
        when(outlines.findByStoryId(5L)).thenReturn(Optional.empty());
        adapter.save(5L, new StoryOutline(List.of(new EpisodicCastMember("Bruno", "amigo")), List.of(beat("uno"), beat("dos"))));
        var captor = ArgumentCaptor.forClass(StoryOutlineJpaEntity.class);
        verify(outlines).save(captor.capture());
        when(outlines.findByStoryId(5L)).thenReturn(Optional.of(captor.getValue()));

        var script = adapter.find(5L).orElseThrow();

        assertEquals(List.of(new EpisodicCastMember("Bruno", "amigo")), script.cast());
        assertEquals(List.of(beat("uno"), beat("dos")), script.beats());
    }

    @Test
    void castMembersAreStoredOnlyInsideTheOutline() {
        // El adaptador solo depende de los repositorios de story y outline: no puede dar de alta
        // personajes de catalogo ni de story. El reparto cuelga de la escaleta.
        StoryJpaEntity story = story(5L, "t", "s");
        when(stories.findById(5L)).thenReturn(Optional.of(story));
        when(outlines.findByStoryId(5L)).thenReturn(Optional.empty());

        adapter.save(5L, new StoryOutline(List.of(new EpisodicCastMember("Bruno", "amigo")), List.of(beat("uno"))));

        var captor = ArgumentCaptor.forClass(StoryOutlineJpaEntity.class);
        verify(outlines).save(captor.capture());
        assertEquals("Bruno", captor.getValue().getCast().get(0).getName());
        assertEquals("amigo", captor.getValue().getCast().get(0).getRole());
        assertTrue(story.getCharacters().isEmpty());
    }

    @Test
    void oldStoryReadsWithoutOutlineOrVersion() {
        StoryJpaEntity old = story(1L, "viejo", "sin escaleta");
        when(stories.findById(1L)).thenReturn(Optional.of(old));
        when(outlines.findByStoryId(1L)).thenReturn(Optional.empty());

        assertTrue(adapter.find(1L).isEmpty());
        assertTrue(adapter.scriptVersion(1L).isEmpty());
    }

    @Test
    void newStoryReportsItsVersion() {
        StoryJpaEntity story = story(2L, "nuevo", "con escaleta");
        story.setScriptVersion(1);
        when(stories.findById(2L)).thenReturn(Optional.of(story));

        assertEquals(Optional.of(1), adapter.scriptVersion(2L));
    }

    @Test
    void savingANewScriptDoesNotTouchAnOldStory() {
        StoryJpaEntity old = story(1L, "viejo", "sin escaleta");
        StoryJpaEntity fresh = story(2L, "nuevo", "con escaleta");
        when(stories.findById(2L)).thenReturn(Optional.of(fresh));
        when(outlines.findByStoryId(2L)).thenReturn(Optional.empty());

        adapter.save(2L, new StoryOutline(List.of(), List.of(beat("uno"))));

        assertNull(old.getScriptVersion());
        verify(stories, never()).findById(1L);
        verify(outlines, never()).findByStoryId(1L);
        verify(stories).save(any(StoryJpaEntity.class));
    }

    @Test
    void replacingAnExistingOutlineDeletesThePreviousOneFirst() {
        StoryJpaEntity story = story(5L, "t", "s");
        var previous = new StoryOutlineJpaEntity();
        when(stories.findById(5L)).thenReturn(Optional.of(story));
        when(outlines.findByStoryId(5L)).thenReturn(Optional.of(previous));

        adapter.save(5L, new StoryOutline(List.of(), List.of(beat("uno"))));

        var order = org.mockito.Mockito.inOrder(outlines);
        order.verify(outlines).delete(previous);
        order.verify(outlines).flush();
        order.verify(outlines).save(any(StoryOutlineJpaEntity.class));
    }

    @Test
    void incompleteOutlineIsRejectedBeforePersisting() {
        var incomplete = new StoryBeat("uno", "funcion", List.of("Luna"), "", "bosque");

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> adapter.save(5L, new StoryOutline(List.of(), List.of(incomplete))));

        verify(outlines, never()).save(any(StoryOutlineJpaEntity.class));
        verify(stories, never()).save(any(StoryJpaEntity.class));
    }

    private StoryJpaEntity story(Long id, String title, String synopsis) {
        var story = new StoryJpaEntity();
        story.setId(id);
        story.setTitle(title);
        story.setSynopsis(synopsis);
        return story;
    }

    private StoryBeat beat(String event) {
        return new StoryBeat(event, "funcion", List.of("Luna", "Bruno"), "alegre", "bosque");
    }
}
