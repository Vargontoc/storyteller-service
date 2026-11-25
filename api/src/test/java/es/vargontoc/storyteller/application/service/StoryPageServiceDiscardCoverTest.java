package es.vargontoc.storyteller.application.service;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;

import es.vargontoc.storyteller.application.ports.in.generator.StoryNarrationUseCase;
import es.vargontoc.storyteller.application.ports.out.GenerationGatePort;
import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.application.ports.out.external.WriterAgent;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageReviewRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageVersionRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.domain.enums.PageStatus;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.shared.exceptions.AppException;

/** TASK-012: descarte de la portada al rehacerse el guion. */
class StoryPageServiceDiscardCoverTest {

    private static final Long STORY = 5L;
    private static final Long COVER = 40L;

    private StoryPageRepository pages;
    private StoryRepository stories;
    private GenerationGatePort gate;
    private StoryPageVersionRepository versions;
    private ResourceStorage storage;
    private StoryPageService service;

    @BeforeEach
    void setUp() {
        pages = mock(StoryPageRepository.class);
        stories = mock(StoryRepository.class);
        gate = mock(GenerationGatePort.class);
        versions = mock(StoryPageVersionRepository.class);
        storage = mock(ResourceStorage.class);
        service = new StoryPageService(mock(OllamaPort.class), "writer", mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS),
            pages, mock(StoryPageReviewRepository.class), stories, mock(StoryOutlineRepository.class), gate,
            mock(WriterAgent.class), versions, mock(StoryPageNotifier.class), Runnable::run,
            mock(StoryNarrationUseCase.class), storage, new ImageContentInspector());
    }

    @Test
    void acceptedCoverLosesVersionsPageAndImportedImageButInteriorPagesStay() {
        when(stories.getStory(STORY)).thenReturn(story(PageStatus.ACCEPTED, true));

        service.discardCover(STORY);

        verify(versions).deleteByPage(COVER);
        verify(pages).delete(COVER);
        verify(storage).deleteImportedImage(STORY, COVER);
        verify(pages, never()).delete(41L);
        verify(pages, never()).deleteAfter(anyLong(), org.mockito.ArgumentMatchers.anyInt());
        verify(pages, never()).deleteByStory(anyLong());
    }

    @Test
    void pendingCoverLosesItsVersions() {
        when(stories.getStory(STORY)).thenReturn(story(PageStatus.PENDING, false));

        service.discardCover(STORY);

        verify(versions).deleteByPage(COVER);
        verify(pages).delete(COVER);
        verify(storage).deleteImportedImage(STORY, COVER);
    }

    @Test
    void storyWithoutCoverDoesNothing() {
        Story story = new Story();
        story.setId(STORY);
        story.setPages(new ArrayList<>());
        when(stories.getStory(STORY)).thenReturn(story);

        service.discardCover(STORY);

        verifyNoInteractions(pages, versions, storage, gate);
    }

    @Test
    void generationInProgressRejectsWithoutTouchingAnything() {
        when(stories.getStory(STORY)).thenReturn(story(PageStatus.PENDING, false));
        when(gate.isGeneratingStory(STORY)).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> service.discardCover(STORY));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verifyNoInteractions(pages, versions, storage);
    }

    private static Story story(PageStatus coverStatus, boolean withInterior) {
        Story story = new Story();
        story.setId(STORY);
        List<StoryPage> list = new ArrayList<>();
        StoryPage cover = new StoryPage();
        cover.setId(COVER);
        cover.setPage(0);
        cover.setPageStatus(coverStatus);
        list.add(cover);
        if (withInterior) {
            StoryPage first = new StoryPage();
            first.setId(41L);
            first.setPage(1);
            list.add(first);
        }
        story.setPages(list);
        return story;
    }
}
