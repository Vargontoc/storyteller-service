package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.domain.enums.StoryStatus;
import es.vargontoc.storyteller.domain.model.CoverPlan;
import es.vargontoc.storyteller.domain.model.EpisodicCastMember;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryBeat;
import es.vargontoc.storyteller.domain.model.StoryOutline;
import es.vargontoc.storyteller.domain.model.StoryScriptVersion;
import es.vargontoc.storyteller.domain.response.StoryScript;
import es.vargontoc.storyteller.shared.exceptions.ResourceNotFoundException;

class StoryScriptServiceTest {

    private static final Long STORY = 5L;

    private StoryRepository stories;
    private StoryOutlineRepository outlines;
    private StoryScriptService service;

    @BeforeEach
    void setUp() {
        stories = mock(StoryRepository.class);
        outlines = mock(StoryOutlineRepository.class);
        service = new StoryScriptService(stories, outlines);
    }

    @Test
    void newStoryReturnsOutlineWithOrderedBeatsAndPageFromOne() {
        when(stories.getStory(STORY)).thenReturn(story(StoryScriptVersion.OUTLINE, StoryStatus.SCRIPT_ACCEPTED));
        when(outlines.find(STORY)).thenReturn(Optional.of(new StoryOutline(
            List.of(new EpisodicCastMember("Bruno", "amigo del bosque")),
            List.of(beat("Luna sale"), beat("Luna encuentra una piedra"), beat("Luna vuelve")))));

        StoryScript script = service.getScript(STORY);

        assertEquals(STORY, script.id());
        assertEquals("Luna y la piedra", script.title());
        assertEquals("Un paseo", script.synopsis());
        assertEquals(StoryStatus.SCRIPT_ACCEPTED, script.status());
        assertEquals(StoryScriptVersion.OUTLINE, script.scriptVersion());
        assertEquals(List.of(new EpisodicCastMember("Bruno", "amigo del bosque")), script.cast());
        assertEquals(3, script.beats().size());
        assertEquals(1, script.beats().get(0).page());
        assertEquals("Luna sale", script.beats().get(0).event());
        assertEquals(2, script.beats().get(1).page());
        assertEquals("Luna encuentra una piedra", script.beats().get(1).event());
        assertEquals(3, script.beats().get(2).page());
        assertEquals(List.of("Luna"), script.beats().get(2).participants());
        assertEquals("curioso", script.beats().get(2).tone());
        assertEquals("bosque", script.beats().get(2).visualNote());
        assertEquals("nudo", script.beats().get(2).narrativeFunction());
    }

    @Test
    void scriptExposesBeatSettingAndCoverPlan() {
        when(stories.getStory(STORY)).thenReturn(story(StoryScriptVersion.OUTLINE, StoryStatus.SCRIPT_ACCEPTED));
        when(outlines.find(STORY)).thenReturn(Optional.of(new StoryOutline(List.of(),
            List.of(new StoryBeat("Luna sale", "inicio", List.of("Luna"), "alegre", "bosque", "cocina de la abuela")),
            new CoverPlan("habitacion al anochecer", "calma"))));

        StoryScript script = service.getScript(STORY);

        assertEquals("cocina de la abuela", script.beats().get(0).setting());
        assertEquals(new CoverPlan("habitacion al anochecer", "calma"), script.cover());
    }

    @Test
    void legacyOutlineHasNullSettingAndCover() {
        when(stories.getStory(STORY)).thenReturn(story(StoryScriptVersion.OUTLINE, StoryStatus.SCRIPT_ACCEPTED));
        when(outlines.find(STORY)).thenReturn(Optional.of(new StoryOutline(List.of(), List.of(beat("Luna sale")))));

        StoryScript script = service.getScript(STORY);

        assertNull(script.beats().get(0).setting());
        assertNull(script.cover());
    }

    @Test
    void oldStoryReturnsEmptyCastAndBeatsWithNullVersion() {
        when(stories.getStory(STORY)).thenReturn(story(null, StoryStatus.SCRIPT_PENDING));

        StoryScript script = service.getScript(STORY);

        assertNull(script.scriptVersion());
        assertEquals(StoryStatus.SCRIPT_PENDING, script.status());
        assertTrue(script.cast().isEmpty());
        assertTrue(script.beats().isEmpty());
        assertNull(script.cover());
        verifyNoInteractions(outlines);
    }

    @Test
    void missingStoryPropagatesNotFound() {
        when(stories.getStory(STORY)).thenThrow(new ResourceNotFoundException("Story not found with id: 5"));

        assertThrows(ResourceNotFoundException.class, () -> service.getScript(STORY));
        verifyNoInteractions(outlines);
    }

    private static Story story(Integer version, StoryStatus status) {
        Story story = new Story();
        story.setId(STORY);
        story.setTitle("Luna y la piedra");
        story.setSynopsis("Un paseo");
        story.setStatus(status);
        story.setScriptVersion(version);
        return story;
    }

    private static StoryBeat beat(String event) {
        return new StoryBeat(event, "nudo", List.of("Luna"), "curioso", "bosque");
    }
}
