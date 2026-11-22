package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
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
import org.mockito.InOrder;
import org.springframework.ai.chat.client.ChatClient;

import es.vargontoc.storyteller.application.ports.in.generator.StoryNarrationUseCase;
import es.vargontoc.storyteller.application.ports.out.GenerationGatePort;
import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.application.ports.out.external.WriterAgent;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageReviewRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageVersionRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.domain.enums.NarrationRejectionReason;
import es.vargontoc.storyteller.domain.enums.PageLengthWarning;
import es.vargontoc.storyteller.domain.enums.PageStatus;
import es.vargontoc.storyteller.domain.enums.StorySize;
import es.vargontoc.storyteller.domain.enums.StoryStatus;
import es.vargontoc.storyteller.domain.model.EpisodicCastMember;
import es.vargontoc.storyteller.domain.model.PageVersion;
import es.vargontoc.storyteller.domain.model.SceneComposition;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryBeat;
import es.vargontoc.storyteller.domain.model.StoryOutline;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.model.StoryScriptVersion;
import es.vargontoc.storyteller.domain.response.WriterHintResult;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import es.vargontoc.storyteller.shared.exceptions.NarrationRejectedException;

/** TASK-024: bloqueo de páginas interiores durante la narración y borrado del audio al reabrir o cambiar. */
class StoryPageServiceNarrationTest {

    private static final Long STORY = 5L;
    private static final Long COVER_ID = 60L;
    private static final Long P2 = 62L;
    private static final Long P3 = 63L;

    private StoryPageRepository pages;
    private StoryPageVersionRepository versions;
    private GenerationGatePort gate;
    private WriterAgent writer;
    private StoryPageNotifier notifier;
    private StoryNarrationUseCase narration;
    private StoryPageService service;

    private final SceneComposition scene = new SceneComposition("Luna", List.of(), "claro");

    @BeforeEach
    void setUp() {
        pages = mock(StoryPageRepository.class);
        StoryRepository stories = mock(StoryRepository.class);
        StoryOutlineRepository outlines = mock(StoryOutlineRepository.class);
        versions = mock(StoryPageVersionRepository.class);
        gate = mock(GenerationGatePort.class);
        when(gate.tryAcquire(any(), any())).thenReturn(true);
        writer = mock(WriterAgent.class);
        notifier = mock(StoryPageNotifier.class);
        narration = mock(StoryNarrationUseCase.class);
        service = new StoryPageService(mock(OllamaPort.class), "writer", mock(ChatClient.class), pages,
            mock(StoryPageReviewRepository.class), stories, outlines, gate, writer, versions, notifier,
            Runnable::run, narration);

        StoryBeat beat = new StoryBeat("e", "nudo", List.of("Luna"), "curioso", "claro");
        when(outlines.find(STORY)).thenReturn(Optional.of(
            new StoryOutline(List.of(new EpisodicCastMember("Conejo", "amigo")), List.of(beat, beat, beat))));
        Story story = story();
        when(stories.getStory(STORY)).thenReturn(story);
        for (StoryPage p : story.getPages())
            when(pages.getPage(p.getId())).thenReturn(p);
        when(writer.rewrite(any(), any(), any())).thenReturn(new WriterHintResult(true, null, "Luna sale. Luna corre.", scene));
        when(pages.accept(anyLong(), any(), any())).thenReturn(page(P2, 2, PageStatus.ACCEPTED));
    }

    // ---- narración en curso: rechazo sin cambios ----

    @Test
    void reopeningAnInteriorPageWhileNarratingIsRejectedAndNothingChanges() {
        when(narration.isNarrating(STORY)).thenReturn(true);

        NarrationRejectedException ex = assertThrows(NarrationRejectedException.class,
            () -> service.startHint(STORY, P2, "hint"));

        assertEquals(NarrationRejectionReason.IN_PROGRESS, ex.getReason());
        verifyNoInteractions(gate, writer, notifier);
        verify(versions, never()).add(anyLong(), any());
        verify(narration, never()).deleteAudio(anyLong());
    }

    @Test
    void regeneratingSelectingAndAcceptingAProposalWhileNarratingAreRejected() {
        when(narration.isNarrating(STORY)).thenReturn(true);

        assertThrows(NarrationRejectedException.class, () -> service.startRegenerate(STORY, P3));
        assertThrows(NarrationRejectedException.class, () -> service.selectVersion(STORY, P2, 1L));
        assertThrows(NarrationRejectedException.class, () -> service.acceptProposal(STORY, P2, true));

        verifyNoInteractions(gate, writer, notifier);
        verify(versions, never()).setCurrent(anyLong(), anyLong());
        verify(pages, never()).accept(anyLong(), any(), any());
        verify(pages, never()).deleteAfter(anyLong(), anyInt());
        verify(narration, never()).deleteAudio(anyLong());
    }

    @Test
    void deletingInvalidatedPagesWhileNarratingIsRejectedAndNothingChanges() {
        when(narration.isNarrating(STORY)).thenReturn(true);

        NarrationRejectedException ex = assertThrows(NarrationRejectedException.class,
            () -> service.deleteInvalidatedPages(STORY));

        assertEquals(NarrationRejectionReason.IN_PROGRESS, ex.getReason());
        verify(pages, never()).deleteByStory(anyLong());
        verify(narration, never()).deleteAudio(anyLong());
    }

    @Test
    void theCoverIsNotBlockedNorDeletesAudio() {
        when(narration.isNarrating(STORY)).thenReturn(true);
        when(narration.hasAudio(STORY)).thenReturn(true);

        service.startHint(STORY, COVER_ID, "más color");

        verify(writer).rewrite(any(), any(), any());
        verify(narration, never()).deleteAudio(anyLong());
    }

    // ---- borrado del audio ----

    @Test
    void reopeningAnInteriorPageOfAStoryWithAudioDeletesAudioAfterLaunching() {
        when(narration.hasAudio(STORY)).thenReturn(true);

        service.startHint(STORY, P2, "hint");

        InOrder order = inOrder(notifier, narration);
        order.verify(notifier).started(STORY, 2);
        order.verify(narration).deleteAudio(STORY);
    }

    @Test
    void acceptingAProposalWithCascadeOfAStoryWithAudioDeletesAudioAfterTheCascade() {
        when(narration.hasAudio(STORY)).thenReturn(true);
        when(versions.findByPage(P2)).thenReturn(List.of(new PageVersion("Propuesta.", scene, PageLengthWarning.NONE, true)));

        service.acceptProposal(STORY, P2, true);

        InOrder order = inOrder(pages, narration);
        order.verify(pages).accept(anyLong(), any(), any());
        order.verify(pages).deleteAfter(STORY, 2);
        order.verify(narration).deleteAudio(STORY);
    }

    @Test
    void acceptingAProposalWithoutConfirmationDoesNotDeleteAudio() {
        when(narration.hasAudio(STORY)).thenReturn(true);
        when(versions.findByPage(P2)).thenReturn(List.of(new PageVersion("Propuesta.", scene, PageLengthWarning.NONE, true)));

        service.acceptProposal(STORY, P2, false);

        verify(pages, never()).accept(anyLong(), any(), any());
        verify(narration, never()).deleteAudio(anyLong());
    }

    @Test
    void selectingAVersionOfAStoryWithAudioDeletesAudio() {
        PageVersion version = new PageVersion("Propuesta.", scene, PageLengthWarning.NONE, false);
        version.setId(9L);
        when(versions.findByPage(P2)).thenReturn(List.of(version));
        when(narration.hasAudio(STORY)).thenReturn(true);

        service.selectVersion(STORY, P2, 9L);

        InOrder order = inOrder(versions, narration);
        order.verify(versions).setCurrent(P2, 9L);
        order.verify(narration).deleteAudio(STORY);
    }

    @Test
    void deletingInvalidatedPagesOfAStoryWithAudioDeletesAudioAfterThePages() {
        when(pages.deleteByStory(STORY)).thenReturn(4);
        when(narration.hasAudio(STORY)).thenReturn(true);

        assertEquals(4, service.deleteInvalidatedPages(STORY));

        InOrder order = inOrder(pages, narration);
        order.verify(pages).deleteByStory(STORY);
        order.verify(narration).deleteAudio(STORY);
    }

    // ---- si la operación de páginas falla, el audio se conserva ----

    @Test
    void ifThePageOperationFailsTheAudioIsKept() {
        when(narration.hasAudio(STORY)).thenReturn(true);
        when(pages.deleteByStory(STORY)).thenThrow(new RuntimeException("bd caída"));
        assertThrows(RuntimeException.class, () -> service.deleteInvalidatedPages(STORY));

        when(gate.tryAcquire(any(), any())).thenReturn(false);
        assertThrows(AppException.class, () -> service.startHint(STORY, P2, "hint"));

        when(versions.findByPage(P2)).thenReturn(List.of(new PageVersion("Propuesta.", scene, PageLengthWarning.NONE, true)));
        when(pages.accept(anyLong(), any(), any())).thenThrow(new RuntimeException("bd caída"));
        assertThrows(RuntimeException.class, () -> service.acceptProposal(STORY, P2, true));

        verify(narration, never()).deleteAudio(anyLong());
    }

    // ---- sin audio: igual que antes ----

    @Test
    void storiesWithoutAudioBehaveAsBefore() {
        when(pages.deleteByStory(STORY)).thenReturn(2);

        service.startHint(STORY, P2, "hint");
        assertEquals(2, service.deleteInvalidatedPages(STORY));

        verify(narration, never()).deleteAudio(anyLong());
        verify(notifier).versionPresented(STORY, 2);
    }

    // ---- helpers ----

    private StoryPage page(Long id, int number, PageStatus status) {
        StoryPage p = new StoryPage();
        p.setId(id);
        p.setStoryId(STORY);
        p.setPage(number);
        p.setPageStatus(status);
        p.setText(number == 0 ? null : "Texto " + number + ".");
        p.setComposition(scene);
        return p;
    }

    /** Cuento con escaleta de tres beats: portada y página 2 aceptadas, página 3 pendiente, 1 y 2 detrás de la portada. */
    private Story story() {
        Story s = new Story();
        s.setId(STORY);
        s.setStatus(StoryStatus.SCRIPT_ACCEPTED);
        s.setScriptVersion(StoryScriptVersion.OUTLINE);
        s.setSize(StorySize.S);
        s.setTitle("Título");
        s.setSynopsis("sinopsis");
        s.setAgentSummary("");
        s.setCharacters(List.of());
        s.setPages(new ArrayList<>(List.of(page(COVER_ID, 0, PageStatus.ACCEPTED), page(61L, 1, PageStatus.ACCEPTED),
            page(P2, 2, PageStatus.ACCEPTED), page(P3, 3, PageStatus.PENDING))));
        return s;
    }
}
