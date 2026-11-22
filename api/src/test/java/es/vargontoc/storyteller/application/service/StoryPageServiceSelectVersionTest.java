package es.vargontoc.storyteller.application.service;

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
import es.vargontoc.storyteller.domain.response.VersionSelected;
import es.vargontoc.storyteller.shared.exceptions.AppException;

/** TASK-017: elegir como vigente una versión del histórico de la portada o de una página pendiente. */
class StoryPageServiceSelectVersionTest {

    private static final Long STORY = 5L;
    /** Ids reales de fila, distintos del número de página y del id de cuento. */
    private static final Long PAGE_ID = 70L;
    private static final Long OTHER_PAGE_ID = 71L;
    private static final Long COVER_ID = 60L;

    private StoryPageRepository pages;
    private InMemoryVersions versions;
    private GenerationGatePort gate;
    private StoryPageService service;

    private final SceneComposition scene1 = new SceneComposition("Primera", List.of(), "claro");
    private final SceneComposition scene3 = new SceneComposition("Tercera", List.of(), "claro");
    private Long first;
    private Long second;
    private Long third;

    @BeforeEach
    void setUp() {
        pages = mock(StoryPageRepository.class);
        versions = new InMemoryVersions();
        gate = mock(GenerationGatePort.class);
        service = new StoryPageService(mock(OllamaPort.class), "writer", mock(ChatClient.class), pages,
            mock(StoryPageReviewRepository.class), mock(StoryRepository.class), mock(StoryOutlineRepository.class),
            gate, mock(WriterAgent.class), versions, mock(StoryPageNotifier.class), Runnable::run);
        when(pages.getPage(PAGE_ID)).thenReturn(page(PAGE_ID, 3, PageStatus.PENDING));
        when(pages.getPage(OTHER_PAGE_ID)).thenReturn(page(OTHER_PAGE_ID, 4, PageStatus.PENDING));
        when(pages.getPage(COVER_ID)).thenReturn(page(COVER_ID, 0, PageStatus.PENDING));
        // Tres versiones con aviso; la tercera es la vigente
        first = versions.add(PAGE_ID, new PageVersion("Uno. Dos. Tres. Cuatro. Cinco.", scene1, PageLengthWarning.SENTENCE_COUNT, false)).getId();
        second = versions.add(PAGE_ID, new PageVersion("Texto largo.", scene1, PageLengthWarning.TOO_MANY_WORDS, false)).getId();
        third = versions.add(PAGE_ID, new PageVersion("Luna sale.", scene3, PageLengthWarning.NONE, true)).getId();
    }

    @Test
    void choosingTheFirstOfThreeMakesItCurrentAndKeepsTheHistory() {
        VersionSelected result = service.selectVersion(STORY, PAGE_ID, first);

        assertEquals(new VersionSelected(STORY, PAGE_ID, first, PageLengthWarning.SENTENCE_COUNT), result);
        List<PageVersion> history = versions.findByPage(PAGE_ID);
        assertEquals(3, history.size());
        assertEquals(List.of(true, false, false), history.stream().map(PageVersion::isCurrent).toList());
        // La página muestra ahora el texto, la escena y el aviso de la elegida
        PageVersion current = history.get(0);
        assertEquals("Uno. Dos. Tres. Cuatro. Cinco.", current.getText());
        assertEquals(scene1, current.getScene());
        assertEquals(PageLengthWarning.SENTENCE_COUNT, current.getWarning());
    }

    @Test
    void chosenVersionIsTheOneAcceptedAfterwards() {
        service.selectVersion(STORY, PAGE_ID, second);

        service.acceptPage(STORY, PAGE_ID);

        verify(pages).accept(PAGE_ID, "Texto largo.", scene1);
        assertTrue(versions.findByPage(PAGE_ID).isEmpty());
    }

    @Test
    void choosingAVersionWithoutSentenceWarningUnblocksTheAcceptance() {
        service.selectVersion(STORY, PAGE_ID, first);
        AppException blocked = assertThrows(AppException.class, () -> service.acceptPage(STORY, PAGE_ID));
        assertEquals(HttpStatus.CONFLICT, blocked.getStatus());
        verify(pages, never()).accept(anyLong(), any(), any());

        service.selectVersion(STORY, PAGE_ID, third);
        service.acceptPage(STORY, PAGE_ID);

        verify(pages).accept(PAGE_ID, "Luna sale.", scene3);
    }

    @Test
    void generationOfTheSameStoryRejectsAndKeepsTheCurrent() {
        when(gate.isGeneratingStory(STORY)).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> service.selectVersion(STORY, PAGE_ID, first));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("generación en curso"));
        assertEquals(third, currentId(PAGE_ID));
    }

    @Test
    void justFinishedGenerationAllowsChoosing() {
        when(gate.isGeneratingStory(STORY)).thenReturn(true, false);
        assertThrows(AppException.class, () -> service.selectVersion(STORY, PAGE_ID, first));

        service.selectVersion(STORY, PAGE_ID, first);

        assertEquals(first, currentId(PAGE_ID));
    }

    @Test
    void choosingDoesNotRequestATurnNorReleaseIt() {
        service.selectVersion(STORY, PAGE_ID, first);

        verify(gate, never()).tryAcquire(any(), any());
        verify(gate, never()).release();
    }

    @Test
    void versionOfAnotherPageIsRejectedWithoutChangingAnything() {
        Long foreign = versions.add(OTHER_PAGE_ID, new PageVersion("Ajena.", scene1, PageLengthWarning.NONE, true)).getId();

        AppException ex = assertThrows(AppException.class, () -> service.selectVersion(STORY, PAGE_ID, foreign));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals(third, currentId(PAGE_ID));
        assertEquals(foreign, currentId(OTHER_PAGE_ID));
    }

    @Test
    void unknownOrNullVersionIsRejected() {
        assertEquals(HttpStatus.NOT_FOUND,
            assertThrows(AppException.class, () -> service.selectVersion(STORY, PAGE_ID, 9999L)).getStatus());
        assertEquals(HttpStatus.NOT_FOUND,
            assertThrows(AppException.class, () -> service.selectVersion(STORY, PAGE_ID, null)).getStatus());
        assertEquals(third, currentId(PAGE_ID));
    }

    @Test
    void pageWithoutStatusIsNotEligible() {
        when(pages.getPage(PAGE_ID)).thenReturn(page(PAGE_ID, 3, null));

        AppException ex = assertThrows(AppException.class, () -> service.selectVersion(STORY, PAGE_ID, first));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals(third, currentId(PAGE_ID));
        verifyNoInteractions(gate);
    }

    @Test
    void reopenedPageAllowsChoosingBetweenItsProposals() {
        when(pages.getPage(PAGE_ID)).thenReturn(page(PAGE_ID, 3, PageStatus.ACCEPTED));

        VersionSelected result = service.selectVersion(STORY, PAGE_ID, first);

        assertEquals(new VersionSelected(STORY, PAGE_ID, first, PageLengthWarning.SENTENCE_COUNT), result);
        assertEquals(first, currentId(PAGE_ID));
        assertEquals(3, versions.findByPage(PAGE_ID).size());
    }

    @Test
    void askingForTheAcceptedVersionOfAReopenedPageIsRejected() {
        // El contenido aceptado no está en el histórico: ni su id de página ni ningún otro id lo identifican como versión
        StoryPage accepted = page(PAGE_ID, 3, PageStatus.ACCEPTED);
        when(pages.getPage(PAGE_ID)).thenReturn(accepted);

        AppException byPageId = assertThrows(AppException.class, () -> service.selectVersion(STORY, PAGE_ID, PAGE_ID));
        AppException unknown = assertThrows(AppException.class, () -> service.selectVersion(STORY, PAGE_ID, 9999L));

        assertEquals(HttpStatus.NOT_FOUND, byPageId.getStatus());
        assertEquals(HttpStatus.NOT_FOUND, unknown.getStatus());
        assertEquals(third, currentId(PAGE_ID));
    }

    @Test
    void acceptedPageWithoutProposalsHasNothingToChoose() {
        when(pages.getPage(OTHER_PAGE_ID)).thenReturn(page(OTHER_PAGE_ID, 4, PageStatus.ACCEPTED));

        AppException ex = assertThrows(AppException.class, () -> service.selectVersion(STORY, OTHER_PAGE_ID, first));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals(third, currentId(PAGE_ID));
    }

    @Test
    void reopenedPageChoosingStillRejectedWhileTheStoryGenerates() {
        when(pages.getPage(PAGE_ID)).thenReturn(page(PAGE_ID, 3, PageStatus.ACCEPTED));
        when(gate.isGeneratingStory(STORY)).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> service.selectVersion(STORY, PAGE_ID, first));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals(third, currentId(PAGE_ID));
    }

    @Test
    void pageOfAnotherStoryIsRejected() {
        StoryPage other = page(PAGE_ID, 3, PageStatus.PENDING);
        other.setStoryId(99L);
        when(pages.getPage(PAGE_ID)).thenReturn(other);

        AppException ex = assertThrows(AppException.class, () -> service.selectVersion(STORY, PAGE_ID, first));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals(third, currentId(PAGE_ID));
    }

    @Test
    void coverVersionCanBeChosenToo() {
        Long v1 = versions.add(COVER_ID, new PageVersion(null, scene1, PageLengthWarning.NONE, false)).getId();
        versions.add(COVER_ID, new PageVersion(null, scene3, PageLengthWarning.NONE, true));

        VersionSelected result = service.selectVersion(STORY, COVER_ID, v1);

        assertEquals(new VersionSelected(STORY, COVER_ID, v1, PageLengthWarning.NONE), result);
        assertEquals(v1, currentId(COVER_ID));
    }

    // ---- helpers ----

    private Long currentId(Long pageId) {
        return versions.findByPage(pageId).stream().filter(PageVersion::isCurrent).findFirst().orElseThrow().getId();
    }

    private static StoryPage page(Long id, int number, PageStatus status) {
        StoryPage p = new StoryPage();
        p.setId(id);
        p.setStoryId(STORY);
        p.setPage(number);
        p.setPageStatus(status);
        return p;
    }

    /** Histórico en memoria con la semántica del repositorio real: una sola vigente por página y setCurrent falla si es ajena. */
    private static class InMemoryVersions implements StoryPageVersionRepository {
        private final List<Long> owners = new ArrayList<>();
        private final List<PageVersion> all = new ArrayList<>();
        private long nextId = 1;

        @Override
        public PageVersion add(Long pageId, PageVersion version) {
            if (version.isCurrent())
                for (int i = 0; i < all.size(); i++)
                    if (owners.get(i).equals(pageId) && all.get(i).isCurrent())
                        all.set(i, copy(all.get(i), false));
            PageVersion saved = copy(version, version.isCurrent());
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
            if (findByPage(pageId).stream().noneMatch(v -> v.getId().equals(versionId)))
                throw new IllegalArgumentException("La version " + versionId + " no pertenece a la pagina " + pageId);
            for (int i = 0; i < all.size(); i++)
                if (owners.get(i).equals(pageId))
                    all.set(i, copy(all.get(i), all.get(i).getId().equals(versionId)));
        }

        @Override
        public void deleteByPage(Long pageId) {
            for (int i = all.size() - 1; i >= 0; i--)
                if (owners.get(i).equals(pageId)) {
                    owners.remove(i);
                    all.remove(i);
                }
        }

        private static PageVersion copy(PageVersion v, boolean current) {
            PageVersion c = new PageVersion(v.getText(), v.getScene(), v.getWarning(), current);
            c.setId(v.getId());
            return c;
        }
    }
}
