package es.vargontoc.storyteller.application.service;

import es.vargontoc.storyteller.application.ports.in.generator.StoryNarrationUseCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;

import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.application.ports.out.GenerationGatePort;
import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.application.ports.out.external.WriterAgent;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageReviewRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageVersionRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.domain.enums.PageLengthWarning;
import es.vargontoc.storyteller.domain.enums.PageStatus;
import es.vargontoc.storyteller.domain.enums.StorySize;
import es.vargontoc.storyteller.domain.enums.StoryStatus;
import es.vargontoc.storyteller.domain.model.PageVersion;
import es.vargontoc.storyteller.domain.model.SceneComposition;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryBeat;
import es.vargontoc.storyteller.domain.model.StoryOutline;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.model.StoryScriptVersion;
import es.vargontoc.storyteller.shared.exceptions.AppException;

/** TASK-021: el cuento queda completo al aceptar el último beat y no admite más páginas. */
class StoryPageServiceCompleteTest {

    private static final Long STORY = 5L;
    private static final Long PAGE_ID = 40L;

    private StoryPageRepository pages;
    private StoryRepository stories;
    private StoryOutlineRepository outlines;
    private StoryPageVersionRepository versions;
    private GenerationGatePort gate;
    private WriterAgent writer;
    private StoryPageService service;

    private final SceneComposition scene = new SceneComposition("Escena", List.of(), "claro");

    @BeforeEach
    void setUp() {
        pages = mock(StoryPageRepository.class);
        stories = mock(StoryRepository.class);
        outlines = mock(StoryOutlineRepository.class);
        versions = mock(StoryPageVersionRepository.class);
        gate = mock(GenerationGatePort.class);
        writer = mock(WriterAgent.class);
        service = new StoryPageService(mock(OllamaPort.class), "writer", mock(ChatClient.class), pages,
            mock(StoryPageReviewRepository.class), stories, outlines, gate, writer, versions,
            mock(StoryPageNotifier.class), Runnable::run, mock(StoryNarrationUseCase.class), mock(ResourceStorage.class), new ImageContentInspector());
        // Guion de 8 beats
        List<StoryBeat> beats = new ArrayList<>();
        for (int i = 1; i <= 8; i++)
            beats.add(new StoryBeat("Beat " + i, "nudo", List.of("Luna"), "curioso", "claro"));
        when(outlines.find(STORY)).thenReturn(Optional.of(new StoryOutline(List.of(), beats)));
        when(versions.findByPage(PAGE_ID)).thenReturn(List.of(new PageVersion("Luna sale.", scene, PageLengthWarning.NONE, true)));
        StoryPage accepted = new StoryPage();
        accepted.setPageStatus(PageStatus.ACCEPTED);
        when(pages.accept(any(), any(), any())).thenReturn(accepted);
    }

    @Test
    void acceptingTheLastBeatMarksTheStoryReadyForAudio() {
        when(pages.getPage(PAGE_ID)).thenReturn(pending(8));

        service.acceptPage(STORY, PAGE_ID);

        verify(stories).updateStatus(STORY, StoryStatus.PAGES_ACCEPTED);
    }

    @Test
    void acceptingANonLastPageKeepsTheStatus() {
        when(pages.getPage(PAGE_ID)).thenReturn(pending(7));

        service.acceptPage(STORY, PAGE_ID);

        verify(stories, never()).updateStatus(anyLong(), any());
    }

    @Test
    void acceptingTheCoverKeepsTheStatus() {
        when(pages.getPage(PAGE_ID)).thenReturn(pending(0));

        service.acceptPage(STORY, PAGE_ID);

        verify(stories, never()).updateStatus(anyLong(), any());
    }

    @Test
    void completeStoryRejectsAskingForAnotherPage() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.PAGES_ACCEPTED));

        AppException ex = assertThrows(AppException.class, () -> service.startPage(STORY, 9));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verifyNoInteractions(writer);
        verify(gate, never()).tryAcquire(any(), any());
    }

    @Test
    void completeStoryRejectsRepeatingAnExistingPageNumberAndCover() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.PAGES_ACCEPTED));

        assertEquals(HttpStatus.CONFLICT, assertThrows(AppException.class, () -> service.startPage(STORY, 8)).getStatus());
        assertEquals(HttpStatus.CONFLICT, assertThrows(AppException.class, () -> service.startCover(STORY)).getStatus());
        verify(gate, never()).tryAcquire(any(), any());
    }

    @Test
    void pageNumberAboveTheOutlineIsRejectedWhileIncomplete() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED));

        AppException ex = assertThrows(AppException.class, () -> service.startPage(STORY, 9));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(gate, never()).tryAcquire(any(), any());
    }

    @Test
    void cascadeThatDeletesPagesReturnsACompleteStoryToScriptAccepted() {
        Story story = story(StoryStatus.PAGES_ACCEPTED, 3);
        when(stories.getStory(STORY)).thenReturn(story);
        StoryPage reopened = accepted(2);
        when(pages.getPage(PAGE_ID)).thenReturn(reopened);

        service.acceptProposal(STORY, PAGE_ID, true);

        verify(pages).deleteAfter(STORY, 2);
        verify(stories).updateStatus(STORY, StoryStatus.SCRIPT_ACCEPTED);
    }

    @Test
    void acceptingTheProposalOfTheLastPageKeepsTheStoryComplete() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.PAGES_ACCEPTED, 8));
        StoryPage last = accepted(8);
        when(pages.getPage(PAGE_ID)).thenReturn(last);

        service.acceptProposal(STORY, PAGE_ID, false);

        verify(pages, never()).deleteAfter(anyLong(), org.mockito.ArgumentMatchers.anyInt());
        verify(stories, never()).updateStatus(anyLong(), any());
    }

    // ---- helpers ----

    private StoryPage pending(int number) {
        return page(number, PageStatus.PENDING);
    }

    private StoryPage accepted(int number) {
        // Con propuesta vigente para poder aceptarla
        when(versions.findByPage(PAGE_ID)).thenReturn(List.of(new PageVersion("Nueva.", scene, PageLengthWarning.NONE, true)));
        return page(number, PageStatus.ACCEPTED);
    }

    private static StoryPage page(int number, PageStatus status) {
        StoryPage p = new StoryPage();
        p.setId(PAGE_ID);
        p.setStoryId(STORY);
        p.setPage(number);
        p.setPageStatus(status);
        return p;
    }

    /** Cuento con escaleta y, además de la portada, las páginas 1..lastPage aceptadas. */
    private static Story story(StoryStatus status, int lastPage) {
        Story story = new Story();
        story.setId(STORY);
        story.setStatus(status);
        story.setScriptVersion(StoryScriptVersion.OUTLINE);
        story.setSize(StorySize.S);
        List<StoryPage> list = new ArrayList<>();
        for (int i = 0; i <= lastPage; i++)
            list.add(page(i, PageStatus.ACCEPTED));
        story.setPages(list);
        return story;
    }

    private static Story story(StoryStatus status) {
        return story(status, 8);
    }
}
