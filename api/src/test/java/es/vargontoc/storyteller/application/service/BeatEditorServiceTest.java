package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

import es.vargontoc.storyteller.application.ports.out.external.DirectorAgent;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.domain.command.BeatEditCommand;
import es.vargontoc.storyteller.domain.enums.BeatEditStatus;
import es.vargontoc.storyteller.domain.enums.StoryStatus;
import es.vargontoc.storyteller.domain.model.EpisodicCastMember;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryBeat;
import es.vargontoc.storyteller.domain.model.StoryOutline;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.model.StoryScriptVersion;
import es.vargontoc.storyteller.domain.response.BeatEditResult;
import es.vargontoc.storyteller.domain.response.DirectorBeat;
import es.vargontoc.storyteller.domain.response.DirectorBeatVerdict;
import es.vargontoc.storyteller.shared.exceptions.AppException;

class BeatEditorServiceTest {

    private static final Long STORY = 5L;

    private StoryRepository stories;
    private StoryOutlineRepository outlines;
    private StoryPageRepository pages;
    private DirectorAgent director;
    private BeatEditorService service;

    private final StoryOutline outline = new StoryOutline(
        List.of(new EpisodicCastMember("Bruno", "amigo del bosque")),
        List.of(beat("Luna sale"), beat("Luna encuentra una piedra"), beat("Luna vuelve")));

    @BeforeEach
    void setUp() {
        stories = mock(StoryRepository.class);
        outlines = mock(StoryOutlineRepository.class);
        pages = mock(StoryPageRepository.class);
        director = mock(DirectorAgent.class);
        service = new BeatEditorService(stories, outlines, pages, director);
        when(outlines.find(STORY)).thenReturn(Optional.of(outline));
        when(director.validateBeat(any())).thenReturn(new DirectorBeatVerdict(true, null));
    }

    @Test
    void validEditUpdatesBeatAndReturnsToPending() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, 0));

        BeatEditResult result = service.edit(new BeatEditCommand(STORY, 2, beat("Luna regala la piedra"), null));

        assertEquals(BeatEditStatus.APPLIED, result.status());
        ArgumentCaptor<StoryOutline> saved = ArgumentCaptor.forClass(StoryOutline.class);
        verify(outlines).save(Mockito.eq(STORY), saved.capture());
        assertEquals("Luna regala la piedra", saved.getValue().beats().get(1).event());
        assertEquals("Luna sale", saved.getValue().beats().get(0).event());
        verify(stories).updateStatus(STORY, StoryStatus.SCRIPT_PENDING);
        verify(pages, never()).deleteByStory(anyLong());
    }

    @Test
    void ageRejectionKeepsBeatAndReturnsReason() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, 3));
        when(director.validateBeat(any())).thenReturn(new DirectorBeatVerdict(false, "Contenido violento"));

        BeatEditResult result = service.edit(new BeatEditCommand(STORY, 1, beat("Luna pelea"), true));

        assertEquals(BeatEditStatus.REJECTED, result.status());
        assertEquals("Contenido violento", result.reason());
        verify(outlines, never()).save(anyLong(), any());
        verify(stories, never()).updateStatus(anyLong(), any());
        verifyNoInteractions(pages);
    }

    @Test
    void generatedPagesRequireConfirmationWithCounter() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, 4));

        BeatEditResult result = service.edit(new BeatEditCommand(STORY, 3, beat("Luna se queda"), null));

        assertEquals(BeatEditStatus.CONFIRMATION_REQUIRED, result.status());
        assertEquals(4, result.invalidatedPages());
        verify(outlines, never()).save(anyLong(), any());
        verify(stories, never()).updateStatus(anyLong(), any());
        verify(pages, never()).deleteByStory(anyLong());
    }

    @Test
    void confirmingDeletesPagesThenAppliesEdit() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, 4));
        when(pages.deleteByStory(STORY)).thenReturn(4);

        BeatEditResult result = service.edit(new BeatEditCommand(STORY, 3, beat("Luna se queda"), true));

        assertEquals(BeatEditStatus.APPLIED, result.status());
        assertEquals(4, result.invalidatedPages());
        assertEquals(0, result.story().getPages().size());
        InOrder order = Mockito.inOrder(pages, outlines, stories);
        order.verify(pages).deleteByStory(STORY);
        order.verify(outlines).save(Mockito.eq(STORY), any());
        order.verify(stories).updateStatus(STORY, StoryStatus.SCRIPT_PENDING);
    }

    @Test
    void cancellingHasNoEffects() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, 4));

        BeatEditResult result = service.edit(new BeatEditCommand(STORY, 3, beat("Luna se queda"), false));

        assertEquals(BeatEditStatus.CANCELLED, result.status());
        assertNull(result.story());
        verifyNoInteractions(director, pages);
        verify(outlines, never()).save(anyLong(), any());
        verify(stories, never()).updateStatus(anyLong(), any());
    }

    @Test
    void editKeepsEpisodicCastUntouchedAndOutsideCatalog() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_PENDING, 0));

        service.edit(new BeatEditCommand(STORY, 1, beat("Luna conoce a Bruno"), null));

        ArgumentCaptor<StoryOutline> saved = ArgumentCaptor.forClass(StoryOutline.class);
        verify(outlines).save(Mockito.eq(STORY), saved.capture());
        assertEquals(outline.cast(), saved.getValue().cast());
    }

    @Test
    void invalidPageOrIncompleteBeatIsRejectedWithoutDirector() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_PENDING, 0));

        assertThrows(AppException.class, () -> service.edit(new BeatEditCommand(STORY, 0, beat("x"), null)));
        assertThrows(AppException.class, () -> service.edit(new BeatEditCommand(STORY, 4, beat("x"), null)));
        assertThrows(AppException.class,
            () -> service.edit(new BeatEditCommand(STORY, 1, new StoryBeat("x", null, List.of("Luna"), "t", "v"), null)));
        verifyNoInteractions(director);
    }

    @Test
    void oldStoryWithoutOutlineIsNotEditable() {
        Story old = story(StoryStatus.SCRIPT_ACCEPTED, 0);
        old.setScriptVersion(null);
        when(stories.getStory(STORY)).thenReturn(old);

        assertThrows(AppException.class, () -> service.edit(new BeatEditCommand(STORY, 1, beat("x"), null)));
    }

    private static StoryBeat beat(String event) {
        return new StoryBeat(event, "nudo", List.of("Luna"), "curioso", "claro del bosque");
    }

    private static Story story(StoryStatus status, int generatedPages) {
        Story story = new Story();
        story.setId(STORY);
        story.setStatus(status);
        story.setScriptVersion(StoryScriptVersion.OUTLINE);
        story.setPages(new java.util.ArrayList<>(java.util.stream.IntStream.range(0, generatedPages)
            .mapToObj(i -> new StoryPage()).toList()));
        return story;
    }
}
