package es.vargontoc.storyteller.application.service;

import es.vargontoc.storyteller.application.ports.in.generator.StoryNarrationUseCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
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
import es.vargontoc.storyteller.domain.model.PageVersion;
import es.vargontoc.storyteller.domain.model.SceneComposition;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.response.ProposalAccepted;
import es.vargontoc.storyteller.shared.exceptions.AppException;

/** TASK-019: aceptar la propuesta vigente de una portada o página reabierta, con descarte en cascada. */
class StoryPageServiceAcceptProposalTest {

    private static final Long STORY = 5L;
    /** Ids reales de fila, distintos del número de página y del id de cuento. */
    private static final Long COVER_ID = 60L;
    private static final Long PAGE1_ID = 71L;
    private static final Long PAGE2_ID = 72L;
    private static final Long PAGE3_ID = 73L;
    private static final Long PAGE4_ID = 74L;

    private StoryPageRepository pages;
    private StoryRepository stories;
    private InMemoryVersions versions;
    private GenerationGatePort gate;
    private ResourceStorage storage;
    private StoryPageService service;

    private final SceneComposition scene = new SceneComposition("Escena", List.of(), "claro");
    private final SceneComposition proposalScene = new SceneComposition("Escena nueva", List.of(), "claro");
    private StoryPage cover;
    private StoryPage page2;
    private StoryPage accepted2;

    @BeforeEach
    void setUp() {
        pages = mock(StoryPageRepository.class);
        stories = mock(StoryRepository.class);
        versions = new InMemoryVersions();
        gate = mock(GenerationGatePort.class);
        storage = mock(ResourceStorage.class);
        service = new StoryPageService(mock(OllamaPort.class), "writer", mock(ChatClient.class), pages,
            mock(StoryPageReviewRepository.class), stories, mock(StoryOutlineRepository.class),
            gate, mock(WriterAgent.class), versions, mock(StoryPageNotifier.class), Runnable::run, mock(StoryNarrationUseCase.class), storage, new ImageContentInspector());

        cover = page(COVER_ID, 0, PageStatus.ACCEPTED);
        StoryPage page1 = page(PAGE1_ID, 1, PageStatus.ACCEPTED);
        page2 = page(PAGE2_ID, 2, PageStatus.ACCEPTED);
        page2.setImportedImage("pagina2.png");
        StoryPage page3 = page(PAGE3_ID, 3, PageStatus.ACCEPTED);
        page3.setImportedImage("pagina3.png");
        StoryPage page4 = page(PAGE4_ID, 4, PageStatus.ACCEPTED);
        Story story = new Story();
        story.setPages(List.of(cover, page1, page2, page3, page4));
        when(stories.getStory(STORY)).thenReturn(story);
        when(pages.getPage(COVER_ID)).thenReturn(cover);
        when(pages.getPage(PAGE2_ID)).thenReturn(page2);
        accepted2 = page(PAGE2_ID, 2, PageStatus.ACCEPTED);
        when(pages.accept(anyLong(), any(), any())).thenReturn(accepted2);
    }

    @Test
    void withoutConfirmationReportsTheCountAndChangesNothing() {
        versions.add(PAGE2_ID, new PageVersion("Propuesta.", proposalScene, PageLengthWarning.NONE, true));

        ProposalAccepted result = service.acceptProposal(STORY, PAGE2_ID, false);

        assertEquals(new ProposalAccepted(STORY, PAGE2_ID, 2, PageStatus.ACCEPTED, true, 2), result);
        assertEquals(1, versions.findByPage(PAGE2_ID).size());
        verify(pages, never()).accept(anyLong(), any(), any());
        verify(pages, never()).deleteAfter(anyLong(), anyInt());
        verify(pages, never()).removeImportedImage(anyLong());
    }

    @Test
    void withConfirmationReplacesContentClearsHistoryAndDeletesLaterPages() {
        versions.add(PAGE2_ID, new PageVersion("Vieja.", scene, PageLengthWarning.NONE, false));
        versions.add(PAGE2_ID, new PageVersion("Propuesta.", proposalScene, PageLengthWarning.NONE, true));
        // El histórico de otra página no se toca desde aquí (el borrado de las posteriores es del repositorio)
        when(pages.deleteAfter(STORY, 2)).thenReturn(2);

        ProposalAccepted result = service.acceptProposal(STORY, PAGE2_ID, true);

        assertEquals(new ProposalAccepted(STORY, PAGE2_ID, 2, PageStatus.ACCEPTED, false, 2), result);
        // La vigente (no la primera) pasa a contenido aceptado; accept deja la imagen importada a null
        verify(pages).accept(PAGE2_ID, "Propuesta.", proposalScene);
        assertTrue(versions.findByPage(PAGE2_ID).isEmpty());
        // Se borran las páginas posteriores a la 2 por número, con la fila real del cuento
        verify(pages).deleteAfter(STORY, 2);
        // Ninguna generación: ni turno ni liberación
        verify(gate, never()).tryAcquire(any(), any());
        verify(gate, never()).release();
    }

    @Test
    void acceptIsAppliedBeforeDeletingLaterPages() {
        versions.add(PAGE2_ID, new PageVersion("Propuesta.", proposalScene, PageLengthWarning.NONE, true));

        service.acceptProposal(STORY, PAGE2_ID, true);

        InOrder order = inOrder(pages);
        order.verify(pages).accept(PAGE2_ID, "Propuesta.", proposalScene);
        order.verify(pages).deleteAfter(STORY, 2);
    }

    @Test
    void pageWithoutLaterPagesIsAcceptedWithoutConfirmation() {
        StoryPage page4 = page(PAGE4_ID, 4, PageStatus.ACCEPTED);
        when(pages.getPage(PAGE4_ID)).thenReturn(page4);
        versions.add(PAGE4_ID, new PageVersion("Propuesta.", proposalScene, PageLengthWarning.NONE, true));

        ProposalAccepted result = service.acceptProposal(STORY, PAGE4_ID, false);

        assertFalse(result.requiresConfirmation());
        assertEquals(0, result.affectedPages());
        verify(pages).accept(PAGE4_ID, "Propuesta.", proposalScene);
        verify(pages, never()).deleteAfter(anyLong(), anyInt());
        assertTrue(versions.findByPage(PAGE4_ID).isEmpty());
    }

    @Test
    void coverIsReplacedWithoutCascadeNorConfirmation() {
        versions.add(COVER_ID, new PageVersion(null, proposalScene, PageLengthWarning.NONE, true));

        ProposalAccepted result = service.acceptProposal(STORY, COVER_ID, false);

        assertEquals(new ProposalAccepted(STORY, COVER_ID, 0, PageStatus.ACCEPTED, false, 0), result);
        verify(pages).accept(COVER_ID, null, proposalScene);
        verify(pages, never()).deleteAfter(anyLong(), anyInt());
        verify(pages, never()).deleteByStory(anyLong());
        assertTrue(versions.findByPage(COVER_ID).isEmpty());
    }

    @Test
    void acceptingTheCoverRemovesOnlyItsImportedImageAndKeepsInteriorPages() {
        versions.add(COVER_ID, new PageVersion(null, proposalScene, PageLengthWarning.NONE, true));

        service.acceptProposal(STORY, COVER_ID, false);

        verify(storage).deleteImportedImage(STORY, COVER_ID);
        verify(storage, never()).deleteImportedImage(STORY, PAGE1_ID);
        verify(storage, never()).deleteImportedImage(STORY, PAGE2_ID);
        verify(storage, never()).deleteImportedImage(STORY, PAGE3_ID);
        verify(storage, never()).deletePageAssets(anyLong(), anyLong(), any());
        verify(pages, never()).deleteAfter(anyLong(), anyInt());
        verify(pages, never()).deleteByStory(anyLong());
    }

    @Test
    void acceptingAnInteriorPageRemovesItsImageAndTheCascadeButNotTheCoverImage() {
        versions.add(PAGE2_ID, new PageVersion("Propuesta.", proposalScene, PageLengthWarning.NONE, true));

        service.acceptProposal(STORY, PAGE2_ID, true);

        verify(storage).deleteImportedImage(STORY, PAGE2_ID);
        // Las posteriores (3 y 4) las retira el repositorio con sus assets; la portada no se toca
        verify(pages).deleteAfter(STORY, 2);
        verify(storage, never()).deleteImportedImage(STORY, COVER_ID);
    }

    @Test
    void rejectedProposalKeepsTheImportedImage() {
        versions.add(PAGE2_ID, new PageVersion("Uno. Dos. Tres. Cuatro. Cinco.", scene, PageLengthWarning.SENTENCE_COUNT, true));

        assertThrows(AppException.class, () -> service.acceptProposal(STORY, PAGE2_ID, true));

        verify(storage, never()).deleteImportedImage(anyLong(), anyLong());
    }

    @Test
    void currentProposalWithSentenceWarningIsRejectedBeforeAnyChange() {
        versions.add(PAGE2_ID, new PageVersion("Uno. Dos. Tres. Cuatro. Cinco.", scene, PageLengthWarning.SENTENCE_COUNT, true));

        AppException ex = assertThrows(AppException.class, () -> service.acceptProposal(STORY, PAGE2_ID, true));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(pages, never()).accept(anyLong(), any(), any());
        verify(pages, never()).deleteAfter(anyLong(), anyInt());
        assertEquals(1, versions.findByPage(PAGE2_ID).size());
    }

    @Test
    void proposalWithOnlyWordWarningCanBeAccepted() {
        versions.add(PAGE2_ID, new PageVersion("Muy larga.", proposalScene, PageLengthWarning.TOO_MANY_WORDS, true));

        ProposalAccepted result = service.acceptProposal(STORY, PAGE2_ID, true);

        assertFalse(result.requiresConfirmation());
        verify(pages).accept(PAGE2_ID, "Muy larga.", proposalScene);
    }

    @Test
    void generationOfTheSameStoryRejectsAndChangesNothing() {
        versions.add(PAGE2_ID, new PageVersion("Propuesta.", proposalScene, PageLengthWarning.NONE, true));
        when(gate.isGeneratingStory(STORY)).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> service.acceptProposal(STORY, PAGE2_ID, true));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("generación en curso"));
        verify(pages, never()).accept(anyLong(), any(), any());
        verify(pages, never()).deleteAfter(anyLong(), anyInt());
        assertEquals(1, versions.findByPage(PAGE2_ID).size());
    }

    @Test
    void acceptedPageWithoutProposalsIsRejected() {
        AppException ex = assertThrows(AppException.class, () -> service.acceptProposal(STORY, PAGE2_ID, true));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(pages, never()).accept(anyLong(), any(), any());
    }

    @Test
    void pendingPageIsRejectedAndKeepsItsHistory() {
        StoryPage pending = page(PAGE2_ID, 2, PageStatus.PENDING);
        when(pages.getPage(PAGE2_ID)).thenReturn(pending);
        versions.add(PAGE2_ID, new PageVersion("Uno.", scene, PageLengthWarning.NONE, true));

        AppException ex = assertThrows(AppException.class, () -> service.acceptProposal(STORY, PAGE2_ID, true));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals(1, versions.findByPage(PAGE2_ID).size());
    }

    @Test
    void pageOfAnotherStoryIsRejectedWithNotFound() {
        StoryPage other = page(PAGE2_ID, 2, PageStatus.ACCEPTED);
        other.setStoryId(99L);
        when(pages.getPage(PAGE2_ID)).thenReturn(other);
        versions.add(PAGE2_ID, new PageVersion("Uno.", scene, PageLengthWarning.NONE, true));

        AppException ex = assertThrows(AppException.class, () -> service.acceptProposal(STORY, PAGE2_ID, true));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(pages, never()).accept(anyLong(), any(), any());
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

    /** Histórico en memoria con la semántica del repositorio real. */
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
