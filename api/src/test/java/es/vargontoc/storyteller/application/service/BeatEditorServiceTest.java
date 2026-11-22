package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import org.springframework.http.HttpStatus;

import es.vargontoc.storyteller.application.ports.in.generator.StoryNarrationUseCase;
import es.vargontoc.storyteller.domain.enums.NarrationRejectionReason;
import es.vargontoc.storyteller.shared.exceptions.NarrationRejectedException;
import es.vargontoc.storyteller.application.ports.out.GenerationGatePort.GenerationType;
import es.vargontoc.storyteller.application.ports.out.external.DirectorAgent;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
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
    private StoryPageService pages;
    private DirectorAgent director;
    private GenerationGate gate;
    private StoryNarrationUseCase narration;
    private BeatEditorService service;

    private final StoryOutline outline = new StoryOutline(
        List.of(new EpisodicCastMember("Bruno", "amigo del bosque")),
        List.of(beat("Luna sale"), beat("Luna encuentra una piedra"), beat("Luna vuelve")));

    @BeforeEach
    void setUp() {
        stories = mock(StoryRepository.class);
        outlines = mock(StoryOutlineRepository.class);
        pages = mock(StoryPageService.class);
        director = mock(DirectorAgent.class);
        gate = new GenerationGate();
        narration = mock(StoryNarrationUseCase.class);
        service = new BeatEditorService(stories, outlines, pages, director, gate, narration);
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
        verify(pages, never()).deleteInvalidatedPages(anyLong());
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
        verify(pages, never()).deleteInvalidatedPages(anyLong());
    }

    @Test
    void confirmingDeletesPagesThenAppliesEdit() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, 4));
        when(pages.deleteInvalidatedPages(STORY)).thenReturn(4);

        BeatEditResult result = service.edit(new BeatEditCommand(STORY, 3, beat("Luna se queda"), true));

        assertEquals(BeatEditStatus.APPLIED, result.status());
        assertEquals(4, result.invalidatedPages());
        assertEquals(0, result.story().getPages().size());
        InOrder order = Mockito.inOrder(pages, outlines, stories);
        order.verify(pages).deleteInvalidatedPages(STORY);
        order.verify(outlines).save(Mockito.eq(STORY), any());
        order.verify(stories).updateStatus(STORY, StoryStatus.SCRIPT_PENDING);
    }

    @Test
    void warningIndicatesAudioDeletionOnlyWhenStoryHasAudio() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, 4));
        when(narration.hasAudio(STORY)).thenReturn(true);

        BeatEditResult withAudio = service.edit(new BeatEditCommand(STORY, 3, beat("Luna se queda"), null));

        assertEquals(BeatEditStatus.CONFIRMATION_REQUIRED, withAudio.status());
        assertEquals(4, withAudio.invalidatedPages());
        assertTrue(withAudio.audioWillBeDeleted());

        when(narration.hasAudio(STORY)).thenReturn(false);

        BeatEditResult withoutAudio = service.edit(new BeatEditCommand(STORY, 3, beat("Luna se queda"), null));

        assertEquals(BeatEditStatus.CONFIRMATION_REQUIRED, withoutAudio.status());
        assertFalse(withoutAudio.audioWillBeDeleted());
    }

    @Test
    void confirmingWithAudioDeletesThroughPageServiceOnlyOnce() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, 4));
        when(narration.hasAudio(STORY)).thenReturn(true);
        when(pages.deleteInvalidatedPages(STORY)).thenReturn(4);

        BeatEditResult result = service.edit(new BeatEditCommand(STORY, 3, beat("Luna se queda"), true));

        assertEquals(BeatEditStatus.APPLIED, result.status());
        verify(pages).deleteInvalidatedPages(STORY); // borra páginas, audio y voz en la misma transacción
        verify(narration, never()).deleteAudio(anyLong());
    }

    @Test
    void editWhileNarratingIsRejectedBeforeChangingAnything() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, 4));
        when(narration.isNarrating(STORY)).thenReturn(true);

        NarrationRejectedException ex = assertThrows(NarrationRejectedException.class,
            () -> service.edit(new BeatEditCommand(STORY, 3, beat("Luna se queda"), true)));

        assertEquals(NarrationRejectionReason.IN_PROGRESS, ex.getReason());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verifyNoInteractions(director, pages);
        verify(outlines, never()).save(anyLong(), any());
        verify(stories, never()).updateStatus(anyLong(), any());
        assertTrue(gate.tryAcquire(GenerationType.WRITER, STORY)); // no dejó el turno ocupado
    }

    @Test
    void editWhileNarratingWithoutGeneratedPagesIsNotRejected() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, 0));
        when(narration.isNarrating(STORY)).thenReturn(true);

        assertEquals(BeatEditStatus.APPLIED,
            service.edit(new BeatEditCommand(STORY, 1, beat("Luna sale de casa"), null)).status());
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

    @Test
    void busyGateRejectsWithConflictAndChangesNothing() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, 4));
        gate.tryAcquire(GenerationType.WRITER, STORY);

        AppException ex = assertThrows(AppException.class,
            () -> service.edit(new BeatEditCommand(STORY, 3, beat("Luna se queda"), true)));

        assertEquals("Hay una generación en curso", ex.getMessage());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verifyNoInteractions(director, pages);
        verify(outlines, never()).save(anyLong(), any());
        verify(stories, never()).updateStatus(anyLong(), any());
        assertTrue(gate.isGeneratingStory(STORY)); // el turno ajeno sigue intacto
    }

    @Test
    void gateIsHeldWhileValidatingAndRejectsOtherGenerations() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, 0));
        boolean[] otherAcquired = {true};
        boolean[] heldAsDirector = {false};
        when(director.validateBeat(any())).thenAnswer(inv -> {
            heldAsDirector[0] = gate.isGeneratingStory(STORY);
            otherAcquired[0] = gate.tryAcquire(GenerationType.WRITER, STORY);
            return new DirectorBeatVerdict(true, null);
        });

        service.edit(new BeatEditCommand(STORY, 1, beat("Luna sale de casa"), null));

        assertTrue(heldAsDirector[0]);
        assertFalse(otherAcquired[0]);
    }

    @Test
    void gateIsFreeAfterApplyRejectionConfirmationAndCancel() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, 3));

        service.edit(new BeatEditCommand(STORY, 1, beat("a"), null)); // pide confirmación
        assertTrue(gate.tryAcquire(GenerationType.WRITER, STORY));
        gate.release();

        service.edit(new BeatEditCommand(STORY, 1, beat("a"), true)); // aplica
        assertTrue(gate.tryAcquire(GenerationType.WRITER, STORY));
        gate.release();

        when(director.validateBeat(any())).thenReturn(new DirectorBeatVerdict(false, "No"));
        service.edit(new BeatEditCommand(STORY, 1, beat("a"), true)); // rechazo del director
        assertTrue(gate.tryAcquire(GenerationType.WRITER, STORY));
        gate.release();

        service.edit(new BeatEditCommand(STORY, 1, beat("a"), false)); // cancelación
        assertTrue(gate.tryAcquire(GenerationType.WRITER, STORY));
    }

    @Test
    void gateIsFreeAfterDirectorFailure() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, 0));
        when(director.validateBeat(any())).thenThrow(new IllegalStateException("ollama caído"));

        assertThrows(IllegalStateException.class,
            () -> service.edit(new BeatEditCommand(STORY, 1, beat("a"), null)));

        assertTrue(gate.tryAcquire(GenerationType.WRITER, STORY));
    }

    @Test
    void cancelAndInvalidInputDoNotTouchGate() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, 3));
        gate.tryAcquire(GenerationType.WRITER, STORY);

        assertEquals(BeatEditStatus.CANCELLED,
            service.edit(new BeatEditCommand(STORY, 1, beat("a"), false)).status());
        assertTrue(gate.isGeneratingStory(STORY));
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
