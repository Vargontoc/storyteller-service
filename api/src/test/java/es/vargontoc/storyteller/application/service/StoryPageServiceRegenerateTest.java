package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;

import es.vargontoc.storyteller.application.ports.out.GenerationGatePort;
import es.vargontoc.storyteller.application.ports.out.GenerationGatePort.GenerationType;
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
import es.vargontoc.storyteller.domain.model.Actor;
import es.vargontoc.storyteller.domain.model.EpisodicCastMember;
import es.vargontoc.storyteller.domain.model.PageVersion;
import es.vargontoc.storyteller.domain.model.SceneComposition;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryBeat;
import es.vargontoc.storyteller.domain.model.StoryOutline;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.model.StoryScriptVersion;
import es.vargontoc.storyteller.domain.request.WriterContext;
import es.vargontoc.storyteller.domain.response.GenerationStarted;
import es.vargontoc.storyteller.domain.response.WriterResult;
import es.vargontoc.storyteller.shared.exceptions.AppException;

/** TASK-016: regeneración sin hint de una página pendiente cuya vigente avisa de número de frases. */
class StoryPageServiceRegenerateTest {

    private static final Long STORY = 5L;
    /** Ids reales de fila de página, distintos del número de página y del id de cuento. */
    private static final Long PAGE_ID = 70L;
    private static final Long COVER_ID = 60L;

    private static final String VALID = "Luna sale. Luna corre.";
    private static final String FIVE_SENTENCES = "Uno. Dos. Tres. Cuatro. Cinco.";
    private static final String TOO_LONG = "palabra ".repeat(69) + "fin.";

    private StoryPageRepository pages;
    private StoryRepository stories;
    private StoryOutlineRepository outlines;
    private InMemoryVersions versions;
    private GenerationGatePort gate;
    private WriterAgent writer;
    private StoryPageNotifier notifier;
    private StoryPageService service;

    private final SceneComposition scene = new SceneComposition("Luna en el claro", List.of(), "claro");
    private final SceneComposition newScene = new SceneComposition("Luna regenerada", List.of(), "claro");
    private final StoryOutline outline = new StoryOutline(List.of(new EpisodicCastMember("Conejo", "amigo")),
        List.of(beat("Luna sale"), beat("Luna encuentra una piedra"), beat("Luna vuelve")));

    @BeforeEach
    void setUp() {
        pages = mock(StoryPageRepository.class);
        stories = mock(StoryRepository.class);
        outlines = mock(StoryOutlineRepository.class);
        versions = new InMemoryVersions();
        gate = mock(GenerationGatePort.class);
        when(gate.tryAcquire(any(), any())).thenReturn(true);
        writer = mock(WriterAgent.class);
        notifier = mock(StoryPageNotifier.class);
        service = new StoryPageService(mock(OllamaPort.class), "writer", mock(ChatClient.class), pages,
            mock(StoryPageReviewRepository.class), stories, outlines, gate, writer, versions, notifier, Runnable::run);
        when(outlines.find(STORY)).thenReturn(Optional.of(outline));
        when(stories.getStory(STORY)).thenReturn(story());
        when(pages.getPage(PAGE_ID)).thenReturn(page(PAGE_ID, 2, PageStatus.PENDING));
        when(pages.getPage(COVER_ID)).thenReturn(page(COVER_ID, 0, PageStatus.PENDING));
        versions.add(PAGE_ID, new PageVersion(FIVE_SENTENCES, scene, PageLengthWarning.SENTENCE_COUNT, true));
        versions.add(COVER_ID, new PageVersion(null, scene, PageLengthWarning.NONE, true));
        when(writer.writePage(any())).thenReturn(new WriterResult(VALID, newScene));
    }

    @Test
    void currentWithSentenceWarningAddsANewCurrentVersionWithoutWarning() {
        GenerationStarted started = service.startRegenerate(STORY, PAGE_ID);

        assertEquals(new GenerationStarted(STORY, 2), started);
        verify(gate).tryAcquire(GenerationType.WRITER, STORY);
        List<PageVersion> history = versions.findByPage(PAGE_ID);
        assertEquals(2, history.size());
        assertEquals(List.of(false, true), history.stream().map(PageVersion::isCurrent).toList());
        assertEquals(VALID, history.get(1).getText());
        assertEquals(newScene, history.get(1).getScene());
        assertEquals(PageLengthWarning.NONE, history.get(1).getWarning());
        verify(writer, times(1)).writePage(any());
        verify(notifier).started(STORY, 2);
        verify(notifier).versionPresented(STORY, 2);
        verify(gate).release();
        // La página real se conserva: ni se acepta ni se crea de nuevo, y no se toca otra página
        verify(pages, never()).accept(anyLong(), any(), any());
        verify(pages, never()).createPending(anyLong(), anyInt(), any());
        assertEquals(1, versions.findByPage(COVER_ID).size());
    }

    @Test
    void bothWarningIsAlsoEligible() {
        versions.add(PAGE_ID, new PageVersion(TOO_LONG + " Dos. Tres. Cuatro. Cinco.", scene, PageLengthWarning.BOTH, true));

        service.startRegenerate(STORY, PAGE_ID);

        assertEquals(3, versions.findByPage(PAGE_ID).size());
        verify(notifier).versionPresented(STORY, 2);
    }

    @Test
    void writerReceivesTheBeatContextOfThePage() {
        service.startRegenerate(STORY, PAGE_ID);

        ArgumentCaptor<WriterContext> context = ArgumentCaptor.forClass(WriterContext.class);
        verify(writer).writePage(context.capture());
        assertEquals(outline.beats().get(1), context.getValue().beat());
        assertEquals(outline.cast(), context.getValue().cast());
        assertTrue(context.getValue().protagonist().contains("pelo azul"));
    }

    @Test
    void outOfLimitGenerationsApplyTheLengthLoopAndWarnings() {
        when(writer.writePage(any())).thenReturn(new WriterResult(TOO_LONG, newScene));

        service.startRegenerate(STORY, PAGE_ID);

        verify(writer, times(3)).writePage(any());
        List<PageVersion> history = versions.findByPage(PAGE_ID);
        assertEquals(4, history.size());
        assertEquals(List.of(false, false, false, true), history.stream().map(PageVersion::isCurrent).toList());
        assertEquals(PageLengthWarning.TOO_MANY_WORDS, history.get(3).getWarning());
        verify(notifier).versionPresented(STORY, 2);
    }

    @Test
    void secondAttemptThatMeetsTheLimitStopsTheLoop() {
        when(writer.writePage(any())).thenReturn(new WriterResult(TOO_LONG, newScene), new WriterResult(VALID, newScene));

        service.startRegenerate(STORY, PAGE_ID);

        verify(writer, times(2)).writePage(any());
        List<PageVersion> history = versions.findByPage(PAGE_ID);
        assertEquals(2, history.size());
        assertEquals(VALID, history.get(1).getText());
    }

    @Test
    void currentWithoutWarningIsRejectedWithoutTurnNorWriter() {
        versions.add(PAGE_ID, new PageVersion(VALID, scene, PageLengthWarning.NONE, true));

        AppException ex = assertThrows(AppException.class, () -> service.startRegenerate(STORY, PAGE_ID));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verifyNoInteractions(gate, writer, notifier);
        assertEquals(2, versions.findByPage(PAGE_ID).size());
    }

    @Test
    void currentWithOnlyWordsWarningIsRejectedEvenIfAnotherVersionHasSentenceWarning() {
        // El histórico conserva una versión con aviso de frases, pero la vigente solo avisa de palabras
        versions.add(PAGE_ID, new PageVersion(TOO_LONG, scene, PageLengthWarning.TOO_MANY_WORDS, true));

        assertThrows(AppException.class, () -> service.startRegenerate(STORY, PAGE_ID));

        verifyNoInteractions(gate, writer, notifier);
        assertEquals(2, versions.findByPage(PAGE_ID).size());
    }

    @Test
    void coverIsNotEligible() {
        AppException ex = assertThrows(AppException.class, () -> service.startRegenerate(STORY, COVER_ID));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verifyNoInteractions(gate, writer, notifier);
    }

    @Test
    void acceptedPageOrPageOfAnotherStoryIsRejectedWithoutTurn() {
        when(pages.getPage(PAGE_ID)).thenReturn(page(PAGE_ID, 2, PageStatus.ACCEPTED));
        assertThrows(AppException.class, () -> service.startRegenerate(STORY, PAGE_ID));

        StoryPage other = page(PAGE_ID, 2, PageStatus.PENDING);
        other.setStoryId(99L);
        when(pages.getPage(PAGE_ID)).thenReturn(other);
        AppException ex = assertThrows(AppException.class, () -> service.startRegenerate(STORY, PAGE_ID));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verifyNoInteractions(gate, writer, notifier);
    }

    @Test
    void busyGateRejectsWithoutCallingTheWriterNorChangingAnything() {
        when(gate.tryAcquire(any(), any())).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> service.startRegenerate(STORY, PAGE_ID));

        assertTrue(ex.getMessage().contains("generación en curso"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verifyNoInteractions(writer, notifier);
        verify(gate, never()).release();
        assertEquals(1, versions.findByPage(PAGE_ID).size());
    }

    @Test
    void writerFailurePublishesFailedKeepsHistoryAndReleasesTheGate() {
        when(writer.writePage(any())).thenReturn(new WriterResult(TOO_LONG, newScene))
            .thenThrow(new AppException("ollama caído", HttpStatus.BAD_REQUEST));

        service.startRegenerate(STORY, PAGE_ID);

        List<PageVersion> history = versions.findByPage(PAGE_ID);
        assertEquals(1, history.size());
        assertTrue(history.get(0).isCurrent());
        verify(notifier).failed(STORY, 2);
        verify(notifier, never()).versionPresented(anyLong(), anyInt());
        verify(gate).release();
    }

    @Test
    void gateIsReleasedWhenTheTaskCannotBeScheduled() {
        StoryPageService rejecting = new StoryPageService(mock(OllamaPort.class), "writer", mock(ChatClient.class), pages,
            mock(StoryPageReviewRepository.class), stories, outlines, gate, writer, versions, notifier,
            task -> { throw new RuntimeException("cola llena"); });

        assertThrows(RuntimeException.class, () -> rejecting.startRegenerate(STORY, PAGE_ID));

        verify(gate).release();
        verifyNoInteractions(writer);
    }

    // ---- helpers ----

    private static StoryBeat beat(String event) {
        return new StoryBeat(event, "nudo", List.of("Luna", "Conejo"), "curioso", "claro del bosque");
    }

    private static StoryPage page(Long id, int number, PageStatus status) {
        StoryPage p = new StoryPage();
        p.setId(id);
        p.setStoryId(STORY);
        p.setPage(number);
        p.setPageStatus(status);
        return p;
    }

    /** Cuento con escaleta, portada y página 1 aceptadas y la página 2 pendiente. */
    private static Story story() {
        Story story = new Story();
        story.setId(STORY);
        story.setStatus(StoryStatus.SCRIPT_ACCEPTED);
        story.setScriptVersion(StoryScriptVersion.OUTLINE);
        story.setSize(StorySize.S);
        story.setTitle("Título");
        story.setSynopsis("sinopsis");
        story.setAgentSummary("");
        Actor luna = new Actor();
        luna.setName("Luna");
        luna.setNarrativeDescription("tierna");
        luna.setVisualDescription("pelo azul");
        story.setCharacters(List.of(luna));
        story.setPages(new ArrayList<>(List.of(
            page(COVER_ID, 0, PageStatus.ACCEPTED), page(66L, 1, PageStatus.ACCEPTED), page(PAGE_ID, 2, PageStatus.PENDING))));
        return story;
    }

    /** Histórico en memoria con la semántica del repositorio real: una sola vigente por página. */
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
