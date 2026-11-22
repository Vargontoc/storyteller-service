package es.vargontoc.storyteller.application.service;

import es.vargontoc.storyteller.application.ports.in.generator.StoryNarrationUseCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
import es.vargontoc.storyteller.domain.response.WriterHintResult;
import es.vargontoc.storyteller.domain.response.WriterResult;
import es.vargontoc.storyteller.shared.exceptions.AppException;

/** TASK-015: cambio con hint sobre la portada o una página pendiente. */
class StoryPageServiceHintTest {

    private static final Long STORY = 5L;
    /** Ids reales de fila de página, distintos del número de página y del id de cuento. */
    private static final Long PAGE_ID = 70L;
    private static final Long COVER_ID = 60L;

    private static final String VALID = "Luna sale. Luna corre.";
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
    private final SceneComposition newScene = new SceneComposition("Luna con el conejo contento", List.of(), "claro");
    private final List<EpisodicCastMember> cast = List.of(new EpisodicCastMember("Conejo", "amigo"));
    private final StoryOutline outline = new StoryOutline(cast,
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
            mock(StoryPageReviewRepository.class), stories, outlines, gate, writer, versions, notifier, Runnable::run, mock(StoryNarrationUseCase.class));
        when(outlines.find(STORY)).thenReturn(Optional.of(outline));
        when(stories.getStory(STORY)).thenReturn(story());
        when(pages.getPage(PAGE_ID)).thenReturn(page(PAGE_ID, 2, PageStatus.PENDING));
        when(pages.getPage(COVER_ID)).thenReturn(page(COVER_ID, 0, PageStatus.PENDING));
        versions.add(PAGE_ID, new PageVersion("Base uno.", scene, PageLengthWarning.NONE, true));
        versions.add(COVER_ID, new PageVersion(null, scene, PageLengthWarning.NONE, true));
        when(writer.rewrite(any(), any(), any())).thenReturn(accepted(VALID));
    }

    @Test
    void validHintAddsACompleteCurrentVersionToTheRealPage() {
        GenerationStarted started = service.startHint(STORY, PAGE_ID, "  que el conejo esté contento  ");

        assertEquals(new GenerationStarted(STORY, 2), started);
        verify(gate).tryAcquire(GenerationType.WRITER, STORY);
        List<PageVersion> history = versions.findByPage(PAGE_ID);
        assertEquals(2, history.size());
        PageVersion current = history.stream().filter(PageVersion::isCurrent).findFirst().orElseThrow();
        assertEquals(VALID, current.getText());
        assertEquals(newScene, current.getScene());
        assertEquals(PageLengthWarning.NONE, current.getWarning());
        // El hint llega al escritor sin espacios sobrantes
        verify(writer).rewrite(any(), any(), eq("que el conejo esté contento"));
        verify(notifier).started(STORY, 2);
        verify(notifier).versionPresented(STORY, 2);
        verify(gate).release();
        // Nada se escribe en otra página, y la página ni se acepta ni se crea de nuevo
        assertEquals(1, versions.findByPage(COVER_ID).size());
        verify(pages, never()).accept(anyLong(), any(), any());
        verify(pages, never()).createPending(anyLong(), anyInt(), any());
    }

    @Test
    void writerReceivesTheCurrentVersionAsBaseNotTheLatestOne() {
        versions.add(PAGE_ID, new PageVersion("Segunda.", scene, PageLengthWarning.NONE, true));
        // La vigente pasa a ser la primera (cambio de vigente hecho por el usuario)
        versions.setCurrent(PAGE_ID, versions.findByPage(PAGE_ID).get(0).getId());

        service.startHint(STORY, PAGE_ID, "hint");

        ArgumentCaptor<WriterResult> base = ArgumentCaptor.forClass(WriterResult.class);
        verify(writer).rewrite(any(), base.capture(), any());
        assertEquals("Base uno.", base.getValue().text());
        assertEquals(scene, base.getValue().scene());
    }

    @Test
    void writerReceivesTheBeatContextOfThePage() {
        Story story = story();
        story.getPages().get(1).setText("Texto uno.");
        story.setTitle("El bosque de Luna");
        when(stories.getStory(STORY)).thenReturn(story);

        service.startHint(STORY, PAGE_ID, "hint");

        ArgumentCaptor<WriterContext> context = ArgumentCaptor.forClass(WriterContext.class);
        verify(writer).rewrite(context.capture(), any(), any());
        assertEquals("El bosque de Luna", context.getValue().title());
        assertEquals(outline.beats().get(1), context.getValue().beat());
        assertEquals(cast, context.getValue().cast());
        assertEquals(List.of("Texto uno."), context.getValue().previousTexts());
        assertTrue(context.getValue().protagonist().contains("pelo azul"));
    }

    @Test
    void successiveHintsUseTheNewestCurrentVersionAsBase() {
        when(writer.rewrite(any(), any(), any())).thenReturn(accepted("Hint uno. Bien."), accepted("Hint dos. Bien."));

        service.startHint(STORY, PAGE_ID, "uno");
        service.startHint(STORY, PAGE_ID, "dos");

        ArgumentCaptor<WriterResult> bases = ArgumentCaptor.forClass(WriterResult.class);
        verify(writer, times(2)).rewrite(any(), bases.capture(), any());
        assertEquals("Base uno.", bases.getAllValues().get(0).text());
        assertEquals("Hint uno. Bien.", bases.getAllValues().get(1).text());
        List<PageVersion> history = versions.findByPage(PAGE_ID);
        assertEquals(3, history.size());
        assertEquals(List.of(false, false, true), history.stream().map(PageVersion::isCurrent).toList());
    }

    @Test
    void tenAcceptedHintsOnAPageWithOneVersionLeaveElevenVersions() {
        for (int i = 0; i < 10; i++)
            service.startHint(STORY, PAGE_ID, "hint " + i);

        List<PageVersion> history = versions.findByPage(PAGE_ID);
        assertEquals(11, history.size());
        assertEquals(1, history.stream().filter(PageVersion::isCurrent).count());
        verify(notifier, times(10)).versionPresented(STORY, 2);
    }

    @Test
    void coverHintAddsASingleVersionWithoutLengthValidation() {
        // La portada nunca pasa por el bucle de longitud: una sola llamada, sin texto y sin aviso
        when(writer.rewrite(any(), any(), any())).thenReturn(new WriterHintResult(true, null, null, newScene));

        GenerationStarted started = service.startHint(STORY, COVER_ID, "más color");

        assertEquals(new GenerationStarted(STORY, 0), started);
        verify(writer, times(1)).rewrite(any(), any(), any());
        List<PageVersion> history = versions.findByPage(COVER_ID);
        assertEquals(2, history.size());
        PageVersion current = history.get(1);
        assertTrue(current.isCurrent());
        assertEquals(newScene, current.getScene());
        assertEquals(PageLengthWarning.NONE, current.getWarning());
        ArgumentCaptor<WriterContext> context = ArgumentCaptor.forClass(WriterContext.class);
        verify(writer).rewrite(context.capture(), any(), any());
        assertTrue(context.getValue().isCover());
        verify(notifier).versionPresented(STORY, 0);
    }

    @Test
    void emptyOrBlankHintIsRejectedWithoutTurnNorWriter() {
        AppException empty = assertThrows(AppException.class, () -> service.startHint(STORY, PAGE_ID, ""));
        assertThrows(AppException.class, () -> service.startHint(STORY, PAGE_ID, "   "));
        assertThrows(AppException.class, () -> service.startHint(STORY, PAGE_ID, null));

        assertTrue(empty.getMessage().contains("obligatorio"));
        assertEquals(HttpStatus.BAD_REQUEST, empty.getStatus());
        verifyNoInteractions(gate, writer, notifier);
        assertEquals(1, versions.findByPage(PAGE_ID).size());
    }

    @Test
    void rejectedHintPublishesTheReasonAndChangesNothing() {
        when(writer.rewrite(any(), any(), any())).thenReturn(new WriterHintResult(false, "El lobo no está en el beat", null, null));

        service.startHint(STORY, PAGE_ID, "que aparezca el lobo");

        List<PageVersion> history = versions.findByPage(PAGE_ID);
        assertEquals(1, history.size());
        assertEquals("Base uno.", history.get(0).getText());
        assertTrue(history.get(0).isCurrent());
        verify(notifier).hintRejected(STORY, 2, "El lobo no está en el beat");
        verify(notifier, never()).failed(anyLong(), anyInt());
        verify(notifier, never()).versionPresented(anyLong(), anyInt());
        // Sin regeneración por longitud y con el turno liberado
        verify(writer, times(1)).rewrite(any(), any(), any());
        verify(gate).release();
    }

    @Test
    void rejectedHintOnTheCoverPublishesTheReason() {
        when(writer.rewrite(any(), any(), any())).thenReturn(new WriterHintResult(false, "No apto", null, null));

        service.startHint(STORY, COVER_ID, "algo violento");

        assertEquals(1, versions.findByPage(COVER_ID).size());
        verify(notifier).hintRejected(STORY, 0, "No apto");
        verify(gate).release();
    }

    @Test
    void writerFailurePublishesFailedAndKeepsHistoryAndCurrent() {
        when(writer.rewrite(any(), any(), any())).thenThrow(new AppException("ollama caído", HttpStatus.BAD_REQUEST));

        service.startHint(STORY, PAGE_ID, "hint");

        List<PageVersion> history = versions.findByPage(PAGE_ID);
        assertEquals(1, history.size());
        assertTrue(history.get(0).isCurrent());
        verify(notifier).failed(STORY, 2);
        verify(notifier, never()).hintRejected(anyLong(), anyInt(), any());
        verify(notifier, never()).versionPresented(anyLong(), anyInt());
        verify(gate).release();
    }

    @Test
    void newVersionOutOfLimitAppliesTheLengthLoop() {
        when(writer.rewrite(any(), any(), any())).thenReturn(accepted(TOO_LONG), accepted(VALID));

        service.startHint(STORY, PAGE_ID, "hint");

        verify(writer, times(2)).rewrite(any(), any(), any());
        List<PageVersion> history = versions.findByPage(PAGE_ID);
        assertEquals(2, history.size());
        assertEquals(VALID, history.get(1).getText());
        assertTrue(history.get(1).isCurrent());
    }

    @Test
    void threeOutOfLimitVersionsAreAddedWithTheirWarningAndTheLastIsCurrent() {
        when(writer.rewrite(any(), any(), any())).thenReturn(accepted(TOO_LONG));

        service.startHint(STORY, PAGE_ID, "hint");

        List<PageVersion> history = versions.findByPage(PAGE_ID);
        assertEquals(4, history.size());
        assertEquals(List.of(false, false, false, true), history.stream().map(PageVersion::isCurrent).toList());
        assertEquals(PageLengthWarning.TOO_MANY_WORDS, history.get(3).getWarning());
        verify(notifier).versionPresented(STORY, 2);
        verify(notifier, never()).failed(anyLong(), anyInt());
    }

    @Test
    void failureAfterAnOutOfLimitAttemptAddsNothing() {
        when(writer.rewrite(any(), any(), any())).thenReturn(accepted(TOO_LONG))
            .thenThrow(new AppException("ollama caído", HttpStatus.BAD_REQUEST));

        service.startHint(STORY, PAGE_ID, "hint");

        assertEquals(1, versions.findByPage(PAGE_ID).size());
        verify(notifier).failed(STORY, 2);
        verify(gate).release();
    }

    @Test
    void currentVersionWithWordsWarningStillAllowsAHint() {
        versions.add(PAGE_ID, new PageVersion(TOO_LONG, scene, PageLengthWarning.TOO_MANY_WORDS, true));

        service.startHint(STORY, PAGE_ID, "hint");

        assertEquals(3, versions.findByPage(PAGE_ID).size());
        verify(notifier).versionPresented(STORY, 2);
    }

    @Test
    void busyGateRejectsWithoutCallingTheWriterNorChangingAnything() {
        when(gate.tryAcquire(any(), any())).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> service.startHint(STORY, PAGE_ID, "hint"));

        assertTrue(ex.getMessage().contains("generación en curso"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verifyNoInteractions(writer, notifier);
        verify(gate, never()).release();
        assertEquals(1, versions.findByPage(PAGE_ID).size());
    }

    @Test
    void pageOfAnotherStoryIsRejectedWithoutTurn() {
        StoryPage other = page(PAGE_ID, 2, PageStatus.PENDING);
        other.setStoryId(99L);
        when(pages.getPage(PAGE_ID)).thenReturn(other);
        AppException ex = assertThrows(AppException.class, () -> service.startHint(STORY, PAGE_ID, "hint"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verifyNoInteractions(gate, writer, notifier);
    }

    @Test
    void gateIsReleasedWhenTheTaskCannotBeScheduled() {
        StoryPageService rejecting = new StoryPageService(mock(OllamaPort.class), "writer", mock(ChatClient.class), pages,
            mock(StoryPageReviewRepository.class), stories, outlines, gate, writer, versions, notifier,
            task -> { throw new RuntimeException("cola llena"); }, mock(StoryNarrationUseCase.class));

        assertThrows(RuntimeException.class, () -> rejecting.startHint(STORY, PAGE_ID, "hint"));

        verify(gate).release();
        verifyNoInteractions(writer);
    }

    // ---- helpers ----

    private WriterHintResult accepted(String text) {
        return new WriterHintResult(true, null, text, newScene);
    }

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
