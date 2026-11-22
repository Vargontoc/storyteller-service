package es.vargontoc.storyteller.application.service;

import es.vargontoc.storyteller.application.ports.in.generator.StoryNarrationUseCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import es.vargontoc.storyteller.domain.response.ProposalsDiscarded;
import es.vargontoc.storyteller.shared.exceptions.AppException;

/** TASK-020: descartar las propuestas de una portada o página aceptada reabierta. */
class StoryPageServiceDiscardProposalsTest {

    private static final Long STORY = 5L;
    /** Ids reales de fila, distintos del número de página y del id de cuento. */
    private static final Long COVER_ID = 60L;
    private static final Long PAGE2_ID = 72L;
    private static final Long PAGE3_ID = 73L;
    private static final Long PAGE4_ID = 74L;

    private StoryPageRepository pages;
    private InMemoryVersions versions;
    private GenerationGatePort gate;
    private StoryPageService service;

    private final SceneComposition scene = new SceneComposition("Escena", List.of(), "claro");
    private StoryPage page2;
    private StoryPage cover;

    @BeforeEach
    void setUp() {
        pages = mock(StoryPageRepository.class);
        versions = new InMemoryVersions();
        gate = mock(GenerationGatePort.class);
        service = new StoryPageService(mock(OllamaPort.class), "writer", mock(ChatClient.class), pages,
            mock(StoryPageReviewRepository.class), mock(StoryRepository.class), mock(StoryOutlineRepository.class),
            gate, mock(WriterAgent.class), versions, mock(StoryPageNotifier.class), Runnable::run, mock(StoryNarrationUseCase.class));
        cover = page(COVER_ID, 0, PageStatus.ACCEPTED);
        cover.setImportedImage("portada.png");
        page2 = page(PAGE2_ID, 2, PageStatus.ACCEPTED);
        page2.setImportedImage("pagina2.png");
        when(pages.getPage(COVER_ID)).thenReturn(cover);
        when(pages.getPage(PAGE2_ID)).thenReturn(page2);
        when(pages.getPage(PAGE3_ID)).thenReturn(page(PAGE3_ID, 3, PageStatus.ACCEPTED));
        when(pages.getPage(PAGE4_ID)).thenReturn(page(PAGE4_ID, 4, PageStatus.ACCEPTED));
    }

    @Test
    void threeProposalsOfPage2AreDiscardedAndLaterPagesAndOtherHistoriesStay() {
        versions.add(PAGE2_ID, new PageVersion("Uno.", scene, PageLengthWarning.NONE, false));
        versions.add(PAGE2_ID, new PageVersion("Dos.", scene, PageLengthWarning.NONE, false));
        versions.add(PAGE2_ID, new PageVersion("Tres.", scene, PageLengthWarning.NONE, true));
        // Propuestas de otras páginas y de la portada: no se tocan
        versions.add(PAGE3_ID, new PageVersion("Otra.", scene, PageLengthWarning.NONE, true));
        versions.add(COVER_ID, new PageVersion(null, scene, PageLengthWarning.NONE, true));

        ProposalsDiscarded result = service.discardProposals(STORY, PAGE2_ID);

        assertEquals(new ProposalsDiscarded(STORY, PAGE2_ID, PageStatus.ACCEPTED), result);
        assertTrue(versions.findByPage(PAGE2_ID).isEmpty());
        assertEquals(1, versions.findByPage(PAGE3_ID).size());
        assertEquals(1, versions.findByPage(COVER_ID).size());
        // Solo se lee la página: ni se acepta, ni se borra, ni se modifica ninguna página
        verify(pages).getPage(PAGE2_ID);
        verifyNoMoreInteractions(pages);
    }

    @Test
    void proposalWithSentenceWarningCanBeDiscardedAndAcceptedContentIsKept() {
        page2.setText("Texto aceptado.");
        page2.setComposition(scene);
        versions.add(PAGE2_ID, new PageVersion("Uno. Dos. Tres. Cuatro. Cinco.", scene, PageLengthWarning.SENTENCE_COUNT, true));

        service.discardProposals(STORY, PAGE2_ID);

        assertTrue(versions.findByPage(PAGE2_ID).isEmpty());
        assertEquals("Texto aceptado.", page2.getText());
        assertEquals(scene, page2.getComposition());
        assertEquals(PageStatus.ACCEPTED, page2.getPageStatus());
    }

    @Test
    void importedImageOfTheAcceptedPageIsKept() {
        versions.add(PAGE2_ID, new PageVersion("Uno.", scene, PageLengthWarning.NONE, true));
        versions.add(PAGE2_ID, new PageVersion("Dos.", scene, PageLengthWarning.NONE, true));

        service.discardProposals(STORY, PAGE2_ID);

        assertEquals("pagina2.png", page2.getImportedImage());
    }

    @Test
    void reopenedCoverKeepsItsAcceptedVersionAndImage() {
        cover.setComposition(scene);
        versions.add(COVER_ID, new PageVersion(null, scene, PageLengthWarning.NONE, true));

        ProposalsDiscarded result = service.discardProposals(STORY, COVER_ID);

        assertEquals(new ProposalsDiscarded(STORY, COVER_ID, PageStatus.ACCEPTED), result);
        assertTrue(versions.findByPage(COVER_ID).isEmpty());
        assertEquals("portada.png", cover.getImportedImage());
        assertEquals(scene, cover.getComposition());
        verify(pages, never()).deleteByStory(STORY);
    }

    @Test
    void generationOfTheSameStoryRejectsAndKeepsTheProposals() {
        versions.add(PAGE2_ID, new PageVersion("Uno.", scene, PageLengthWarning.NONE, true));
        when(gate.isGeneratingStory(STORY)).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> service.discardProposals(STORY, PAGE2_ID));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("generación en curso"));
        assertEquals(1, versions.findByPage(PAGE2_ID).size());
    }

    @Test
    void justFinishedGenerationAllowsDiscarding() {
        versions.add(PAGE2_ID, new PageVersion("Uno.", scene, PageLengthWarning.NONE, true));
        when(gate.isGeneratingStory(STORY)).thenReturn(true, false);
        assertThrows(AppException.class, () -> service.discardProposals(STORY, PAGE2_ID));

        service.discardProposals(STORY, PAGE2_ID);

        assertTrue(versions.findByPage(PAGE2_ID).isEmpty());
    }

    @Test
    void discardingDoesNotRequestATurnNorReleaseIt() {
        versions.add(PAGE2_ID, new PageVersion("Uno.", scene, PageLengthWarning.NONE, true));

        service.discardProposals(STORY, PAGE2_ID);

        verify(gate, never()).tryAcquire(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(gate, never()).release();
    }

    @Test
    void acceptedPageWithoutProposalsIsRejected() {
        AppException ex = assertThrows(AppException.class, () -> service.discardProposals(STORY, PAGE2_ID));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void pendingPageIsNotReopenedAndKeepsItsHistory() {
        StoryPage pending = page(PAGE2_ID, 2, PageStatus.PENDING);
        when(pages.getPage(PAGE2_ID)).thenReturn(pending);
        versions.add(PAGE2_ID, new PageVersion("Uno.", scene, PageLengthWarning.NONE, true));

        AppException ex = assertThrows(AppException.class, () -> service.discardProposals(STORY, PAGE2_ID));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals(1, versions.findByPage(PAGE2_ID).size());
    }

    @Test
    void pageOfAnotherStoryIsRejectedWithoutChangingAnything() {
        StoryPage other = page(PAGE2_ID, 2, PageStatus.ACCEPTED);
        other.setStoryId(99L);
        when(pages.getPage(PAGE2_ID)).thenReturn(other);
        versions.add(PAGE2_ID, new PageVersion("Uno.", scene, PageLengthWarning.NONE, true));

        AppException ex = assertThrows(AppException.class, () -> service.discardProposals(STORY, PAGE2_ID));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals(1, versions.findByPage(PAGE2_ID).size());
    }

    // ---- helpers ----

    private static StoryPage page(Long id, int number, PageStatus status) {
        StoryPage p = new StoryPage();
        p.setId(id);
        p.setStoryId(STORY);
        p.setPage(number);
        p.setPageStatus(status);
        return p;
    }

    /** Histórico en memoria con la semántica del repositorio real: solo borra las versiones de la página indicada. */
    private static class InMemoryVersions implements StoryPageVersionRepository {
        private final List<Long> owners = new ArrayList<>();
        private final List<PageVersion> all = new ArrayList<>();
        private long nextId = 1;

        @Override
        public PageVersion add(Long pageId, PageVersion version) {
            PageVersion saved = new PageVersion(version.getText(), version.getScene(), version.getWarning(), version.isCurrent());
            saved.setId(nextId++);
            owners.add(pageId);
            all.add(saved);
            return saved;
        }

        @Override
        public List<PageVersion> findByPage(Long pageId) {
            List<PageVersion> result = new ArrayList<>();
            for (int i = 0; i < all.size(); i++)
                if (owners.get(i).equals(pageId))
                    result.add(all.get(i));
            return result;
        }

        @Override
        public void setCurrent(Long pageId, Long versionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteByPage(Long pageId) {
            for (int i = all.size() - 1; i >= 0; i--)
                if (owners.get(i).equals(pageId)) {
                    owners.remove(i);
                    all.remove(i);
                }
        }
    }
}
