package es.vargontoc.storyteller.application.service;

import es.vargontoc.storyteller.application.ports.in.generator.StoryNarrationUseCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;

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
import es.vargontoc.storyteller.domain.model.PageVersion;
import es.vargontoc.storyteller.domain.model.SceneComposition;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.shared.exceptions.AppException;

class StoryPageServiceAcceptTest {

    private static final Long STORY = 5L;
    private static final Long PAGE = 40L;

    private StoryPageRepository pages;
    private StoryPageVersionRepository versions;
    private GenerationGatePort gate;
    private WriterAgent writer;
    private StoryPageNotifier notifier;
    private StoryPageService service;

    private final SceneComposition scene1 = new SceneComposition("Luna sale", List.of(), "bosque");
    private final SceneComposition scene2 = new SceneComposition("Luna corre", List.of(), "prado");

    @BeforeEach
    void setUp() {
        pages = mock(StoryPageRepository.class);
        versions = mock(StoryPageVersionRepository.class);
        gate = mock(GenerationGatePort.class);
        writer = mock(WriterAgent.class);
        notifier = mock(StoryPageNotifier.class);
        service = new StoryPageService(mock(OllamaPort.class), "writer", mock(ChatClient.class), pages,
            mock(StoryPageReviewRepository.class), mock(StoryRepository.class), mock(StoryOutlineRepository.class),
            gate, writer, versions, notifier, Runnable::run, mock(StoryNarrationUseCase.class));
    }

    private StoryPage pendingPage(Long storyId, int number) {
        StoryPage page = new StoryPage();
        page.setId(PAGE);
        page.setStoryId(storyId);
        page.setPage(number);
        page.setPageStatus(PageStatus.PENDING);
        when(pages.getPage(PAGE)).thenReturn(page);
        return page;
    }

    private PageVersion version(Long id, String text, SceneComposition scene, PageLengthWarning warning, boolean current) {
        PageVersion v = new PageVersion(text, scene, warning, current);
        v.setId(id);
        return v;
    }

    private void acceptedResult() {
        StoryPage accepted = new StoryPage();
        accepted.setId(PAGE);
        accepted.setPageStatus(PageStatus.ACCEPTED);
        when(pages.accept(any(), any(), any())).thenReturn(accepted);
    }

    @Test
    void coverIsAcceptedWithoutTextAndHistoryIsDeleted() {
        pendingPage(STORY, 0);
        when(versions.findByPage(PAGE)).thenReturn(List.of(version(1L, null, scene1, PageLengthWarning.NONE, true)));
        acceptedResult();

        StoryPage result = service.acceptPage(STORY, PAGE);

        assertEquals(PageStatus.ACCEPTED, result.getPageStatus());
        verify(pages).accept(PAGE, null, scene1);
        verify(versions).deleteByPage(PAGE);
        verifyNoInteractions(writer, notifier);
    }

    @Test
    void pageOneIsAcceptedAndNoGenerationStarts() {
        pendingPage(STORY, 1);
        when(versions.findByPage(PAGE)).thenReturn(List.of(version(1L, "Luna sale.", scene1, PageLengthWarning.NONE, true)));
        acceptedResult();

        service.acceptPage(STORY, PAGE);

        verify(pages).accept(PAGE, "Luna sale.", scene1);
        verify(versions).deleteByPage(PAGE);
        verify(gate, never()).tryAcquire(any(), any());
        verifyNoInteractions(writer, notifier);
    }

    @Test
    void currentVersionAmongThreeIsTheAcceptedOne() {
        pendingPage(STORY, 3);
        when(versions.findByPage(PAGE)).thenReturn(List.of(
            version(1L, "Primera.", scene1, PageLengthWarning.NONE, false),
            version(2L, "Segunda.", scene2, PageLengthWarning.NONE, true),
            version(3L, "Tercera.", scene1, PageLengthWarning.NONE, false)));
        acceptedResult();

        service.acceptPage(STORY, PAGE);

        verify(pages).accept(PAGE, "Segunda.", scene2);
        verify(versions).deleteByPage(PAGE);
    }

    @Test
    void wordsWarningDoesNotBlockAndKeepsTheTextUnchanged() {
        pendingPage(STORY, 2);
        when(versions.findByPage(PAGE)).thenReturn(List.of(
            version(1L, "Texto largo sin cambios.", scene1, PageLengthWarning.TOO_MANY_WORDS, true)));
        acceptedResult();

        service.acceptPage(STORY, PAGE);

        verify(pages).accept(PAGE, "Texto largo sin cambios.", scene1);
    }

    @Test
    void sentenceWarningIsRejectedAndNothingChanges() {
        assertSentenceWarningRejected(PageLengthWarning.SENTENCE_COUNT);
    }

    @Test
    void bothWarningsAreRejectedAndNothingChanges() {
        assertSentenceWarningRejected(PageLengthWarning.BOTH);
    }

    private void assertSentenceWarningRejected(PageLengthWarning warning) {
        pendingPage(STORY, 2);
        when(versions.findByPage(PAGE)).thenReturn(List.of(version(1L, "Una. Dos. Tres. Cuatro. Cinco.", scene1, warning, true)));

        AppException ex = assertThrows(AppException.class, () -> service.acceptPage(STORY, PAGE));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("otra versión válida"));
        assertTrue(ex.getMessage().contains("hint"));
        verify(pages, never()).accept(any(), any(), any());
        verify(versions, never()).deleteByPage(anyLong());
    }

    @Test
    void generationOfTheSameStoryIsRejectedAndNothingChanges() {
        pendingPage(STORY, 1);
        when(gate.isGeneratingStory(STORY)).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> service.acceptPage(STORY, PAGE));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("generación en curso"));
        verify(pages, never()).accept(any(), any(), any());
        verify(versions, never()).deleteByPage(anyLong());
    }

    @Test
    void generationOfAnotherStoryDoesNotBlock() {
        pendingPage(STORY, 1);
        when(gate.isGeneratingStory(STORY)).thenReturn(false);
        when(gate.isGeneratingStory(99L)).thenReturn(true);
        when(versions.findByPage(PAGE)).thenReturn(List.of(version(1L, "Luna sale.", scene1, PageLengthWarning.NONE, true)));
        acceptedResult();

        service.acceptPage(STORY, PAGE);

        verify(pages).accept(PAGE, "Luna sale.", scene1);
    }

    @Test
    void alreadyAcceptedPageIsRejected() {
        StoryPage page = pendingPage(STORY, 1);
        page.setPageStatus(PageStatus.ACCEPTED);

        AppException ex = assertThrows(AppException.class, () -> service.acceptPage(STORY, PAGE));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(pages, never()).accept(any(), any(), any());
    }

    @Test
    void pageOfAnotherStoryIsNotFound() {
        pendingPage(77L, 1);

        AppException ex = assertThrows(AppException.class, () -> service.acceptPage(STORY, PAGE));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(pages, never()).accept(any(), any(), any());
    }
}
