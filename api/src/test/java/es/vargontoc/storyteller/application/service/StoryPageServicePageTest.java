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

/** TASK-013: generar la página N desde su beat. */
class StoryPageServicePageTest {

    private static final Long STORY = 5L;

    private StoryPageRepository pages;
    private StoryRepository stories;
    private StoryOutlineRepository outlines;
    private StoryPageVersionRepository versions;
    private GenerationGatePort gate;
    private WriterAgent writer;
    private StoryPageNotifier notifier;
    private StoryPageService service;

    private final SceneComposition scene = new SceneComposition("Luna en el claro", List.of(), "claro");
    private final List<EpisodicCastMember> cast = List.of(new EpisodicCastMember("Conejo", "amigo"));
    private final StoryOutline outline = new StoryOutline(cast,
        List.of(beat("Luna sale"), beat("Luna encuentra una piedra"), beat("Luna vuelve")));

    @BeforeEach
    void setUp() {
        pages = mock(StoryPageRepository.class);
        stories = mock(StoryRepository.class);
        outlines = mock(StoryOutlineRepository.class);
        versions = mock(StoryPageVersionRepository.class);
        gate = mock(GenerationGatePort.class);
        when(gate.tryAcquire(any(), any())).thenReturn(true);
        writer = mock(WriterAgent.class);
        notifier = mock(StoryPageNotifier.class);
        service = new StoryPageService(mock(OllamaPort.class), "writer", mock(ChatClient.class), pages,
            mock(StoryPageReviewRepository.class), stories, outlines, gate, writer, versions, notifier, Runnable::run, mock(StoryNarrationUseCase.class));
        when(outlines.find(STORY)).thenReturn(Optional.of(outline));
        // Ids de fila de story_beat distintos del número de página, como en la base de datos real
        when(outlines.findBeatId(STORY, 1)).thenReturn(Optional.of(501L));
        when(outlines.findBeatId(STORY, 2)).thenReturn(Optional.of(502L));
        StoryPage created = new StoryPage();
        created.setId(70L);
        when(pages.createPending(eq(STORY), anyInt(), any())).thenReturn(created);
        when(writer.writePage(any())).thenReturn(new WriterResult("Luna sale. Luna corre.", scene));
    }

    @Test
    void firstPageStartsAndEndsPendingLinkedToItsBeatWithoutWarning() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, PageStatus.ACCEPTED));

        GenerationStarted started = service.startPage(STORY, 1);

        assertEquals(new GenerationStarted(STORY, 1), started);
        verify(gate).tryAcquire(GenerationType.WRITER, STORY);
        verify(pages).createPending(STORY, 1, 501L);
        ArgumentCaptor<PageVersion> version = ArgumentCaptor.forClass(PageVersion.class);
        verify(versions).add(eq(70L), version.capture());
        assertEquals("Luna sale. Luna corre.", version.getValue().getText());
        assertEquals(scene, version.getValue().getScene());
        assertEquals(PageLengthWarning.NONE, version.getValue().getWarning());
        assertTrue(version.getValue().isCurrent());
        verify(pages, never()).accept(anyLong(), any(), any());
        verify(notifier).started(STORY, 1);
        verify(notifier).versionPresented(STORY, 1);
        verify(notifier, never()).failed(anyLong(), anyInt());
        verify(gate).release();
    }

    @Test
    void pageWithoutARealBeatRowIsNotCreatedAndFailsCleanly() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, PageStatus.ACCEPTED));
        when(outlines.findBeatId(STORY, 1)).thenReturn(Optional.empty());

        service.startPage(STORY, 1);

        verify(pages, never()).createPending(anyLong(), anyInt(), any());
        verify(notifier).failed(STORY, 1);
        verify(gate).release();
    }

    // ---- TASK-014: bucle de longitud ----

    private static final String VALID = "Luna sale. Luna corre.";
    private static final String TOO_LONG = "palabra ".repeat(69) + "fin.";
    private static final String FIVE_SENTENCES = "Una. Dos. Tres. Cuatro. Cinco.";

    private List<PageVersion> savedVersions() {
        ArgumentCaptor<PageVersion> captor = ArgumentCaptor.forClass(PageVersion.class);
        verify(versions, org.mockito.Mockito.atLeastOnce()).add(eq(70L), captor.capture());
        return captor.getAllValues();
    }

    @Test
    void firstValidGenerationIsSavedAloneWithASingleWriterCall() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, PageStatus.ACCEPTED));

        service.startPage(STORY, 1);

        verify(writer, org.mockito.Mockito.times(1)).writePage(any());
        assertEquals(1, savedVersions().size());
        verify(notifier).versionPresented(STORY, 1);
    }

    @Test
    void secondValidGenerationIsTheOnlySavedVersionAndThereIsNoThird() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, PageStatus.ACCEPTED));
        when(writer.writePage(any())).thenReturn(new WriterResult(TOO_LONG, scene), new WriterResult(VALID, scene));

        service.startPage(STORY, 1);

        verify(writer, org.mockito.Mockito.times(2)).writePage(any());
        List<PageVersion> saved = savedVersions();
        assertEquals(1, saved.size());
        assertEquals(VALID, saved.get(0).getText());
        assertEquals(PageLengthWarning.NONE, saved.get(0).getWarning());
        assertTrue(saved.get(0).isCurrent());
    }

    @Test
    void threeTooLongGenerationsAreSavedWithTheWordsWarningAndTheThirdIsCurrent() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, PageStatus.ACCEPTED));
        when(writer.writePage(any())).thenReturn(new WriterResult(TOO_LONG, scene));

        service.startPage(STORY, 1);

        verify(writer, org.mockito.Mockito.times(3)).writePage(any());
        List<PageVersion> saved = savedVersions();
        assertEquals(3, saved.size());
        assertTrue(saved.stream().allMatch(v -> v.getWarning() == PageLengthWarning.TOO_MANY_WORDS));
        assertEquals(List.of(false, false, true), saved.stream().map(PageVersion::isCurrent).toList());
        verify(notifier).versionPresented(STORY, 1);
        verify(notifier, never()).failed(anyLong(), anyInt());
        verify(gate).release();
    }

    @Test
    void threeGenerationsWithFiveSentencesKeepThePageWithSentenceWarnings() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, PageStatus.ACCEPTED));
        when(writer.writePage(any())).thenReturn(new WriterResult(FIVE_SENTENCES, scene));

        service.startPage(STORY, 1);

        List<PageVersion> saved = savedVersions();
        assertEquals(3, saved.size());
        assertTrue(saved.stream().allMatch(v -> v.getWarning() == PageLengthWarning.SENTENCE_COUNT));
        verify(pages, never()).delete(anyLong());
    }

    @Test
    void mixedWarningsKeepEachOneAndTheLastIsCurrent() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, PageStatus.ACCEPTED));
        when(writer.writePage(any())).thenReturn(new WriterResult(FIVE_SENTENCES, scene),
            new WriterResult(TOO_LONG, scene), new WriterResult(TOO_LONG, scene));

        service.startPage(STORY, 1);

        List<PageVersion> saved = savedVersions();
        assertEquals(List.of(PageLengthWarning.SENTENCE_COUNT, PageLengthWarning.TOO_MANY_WORDS, PageLengthWarning.TOO_MANY_WORDS),
            saved.stream().map(PageVersion::getWarning).toList());
        assertTrue(saved.get(2).isCurrent());
    }

    @Test
    void nothingIsVisibleAndTheGateIsHeldWhileRegenerating() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, PageStatus.ACCEPTED));
        // Dentro de la segunda generación: nada persistido todavía y el turno sigue retenido
        when(writer.writePage(any())).thenReturn(new WriterResult(TOO_LONG, scene)).thenAnswer(inv -> {
            verify(pages, never()).createPending(anyLong(), anyInt(), any());
            verify(versions, never()).add(anyLong(), any());
            verify(gate, never()).release();
            verify(notifier, never()).versionPresented(anyLong(), anyInt());
            return new WriterResult(VALID, scene);
        });

        service.startPage(STORY, 1);

        verify(gate, org.mockito.Mockito.times(1)).tryAcquire(any(), any());
        verify(versions).add(eq(70L), any());
        verify(gate).release();
    }

    @Test
    void secondGenerationFailureLeavesNothingPublishesFailedAndReleasesTheGate() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, PageStatus.ACCEPTED));
        when(writer.writePage(any())).thenReturn(new WriterResult(TOO_LONG, scene))
            .thenThrow(new AppException("ollama caído", HttpStatus.BAD_REQUEST));

        service.startPage(STORY, 1);

        verify(writer, org.mockito.Mockito.times(2)).writePage(any());
        verify(pages, never()).createPending(anyLong(), anyInt(), any());
        verify(versions, never()).add(anyLong(), any());
        verify(notifier).failed(STORY, 1);
        verify(notifier, never()).versionPresented(anyLong(), anyInt());
        verify(gate).release();
    }

    @Test
    void pendingOrMissingCoverRejectsPageOneWithoutTurnNorWriter() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, PageStatus.PENDING));
        AppException pending = assertThrows(AppException.class, () -> service.startPage(STORY, 1));
        assertTrue(pending.getMessage().contains("aceptar la portada"));

        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, new PageStatus[] { null }));
        AppException missing = assertThrows(AppException.class, () -> service.startPage(STORY, 1));
        assertTrue(missing.getMessage().contains("aceptar la portada"));

        verifyNoInteractions(gate, writer, notifier);
    }

    @Test
    void pendingPreviousPageRejectsTheNextOne() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, PageStatus.ACCEPTED,
            PageStatus.ACCEPTED, PageStatus.PENDING));

        AppException ex = assertThrows(AppException.class, () -> service.startPage(STORY, 3));

        assertTrue(ex.getMessage().contains("aceptar la página 2"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verifyNoInteractions(gate, writer, notifier);
    }

    @Test
    void secondPageIsGeneratedAloneWhenPageOneIsAccepted() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, PageStatus.ACCEPTED, PageStatus.ACCEPTED));

        service.startPage(STORY, 2);

        verify(pages).createPending(STORY, 2, 502L);
        verify(writer).writePage(any());
        verify(pages, never()).createPending(eq(STORY), eq(3), any());
    }

    @Test
    void existingOrNotNextPageIsRejected() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, PageStatus.ACCEPTED, PageStatus.ACCEPTED));

        assertThrows(AppException.class, () -> service.startPage(STORY, 1));
        assertThrows(AppException.class, () -> service.startPage(STORY, 3));
        assertThrows(AppException.class, () -> service.startPage(STORY, 0));
        assertThrows(AppException.class, () -> service.startPage(STORY, -1));

        verifyNoInteractions(gate, writer, notifier);
    }

    @Test
    void pageBeyondTheOutlineIsRejected() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, PageStatus.ACCEPTED,
            PageStatus.ACCEPTED, PageStatus.ACCEPTED, PageStatus.ACCEPTED));

        AppException ex = assertThrows(AppException.class, () -> service.startPage(STORY, 4));

        assertTrue(ex.getMessage().contains("no tiene beat"));
        verifyNoInteractions(gate, writer, notifier);
    }

    @Test
    void scriptNotApprovedOrOldStoryIsRejected() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_PENDING, PageStatus.ACCEPTED));
        assertThrows(AppException.class, () -> service.startPage(STORY, 1));

        Story old = story(StoryStatus.SCRIPT_ACCEPTED, PageStatus.ACCEPTED);
        old.setScriptVersion(null);
        when(stories.getStory(STORY)).thenReturn(old);
        assertThrows(AppException.class, () -> service.startPage(STORY, 1));

        verifyNoInteractions(gate, writer, notifier);
    }

    @Test
    void writerReceivesTheExactContext() {
        // portada + páginas 1 y 2 aceptadas: toca la 3
        Story story = story(StoryStatus.SCRIPT_ACCEPTED, PageStatus.ACCEPTED, PageStatus.ACCEPTED, PageStatus.ACCEPTED);
        story.setTitle("El bosque de Luna");
        story.setSynopsis("Luna explora");
        story.getPages().get(1).setText("Texto uno.");
        story.getPages().get(2).setText("Texto dos.");
        when(stories.getStory(STORY)).thenReturn(story);

        service.startPage(STORY, 3);

        ArgumentCaptor<WriterContext> captor = ArgumentCaptor.forClass(WriterContext.class);
        verify(writer).writePage(captor.capture());
        WriterContext context = captor.getValue();
        assertEquals("El bosque de Luna", context.title());
        assertEquals("Luna explora", context.synopsis());
        assertEquals(cast, context.cast());
        assertEquals(outline.beats().get(2), context.beat());
        assertEquals(List.of("Texto uno.", "Texto dos."), context.previousTexts());
        assertTrue(context.protagonist().contains("Luna ( tierna / pelo azul )"));
    }

    @Test
    void writerReceivesTheStorySnapshotAndNotTheEditedCatalog() {
        Story story = story(StoryStatus.SCRIPT_ACCEPTED, PageStatus.ACCEPTED);
        when(stories.getStory(STORY)).thenReturn(story);

        service.startPage(STORY, 1);

        ArgumentCaptor<WriterContext> captor = ArgumentCaptor.forClass(WriterContext.class);
        verify(writer).writePage(captor.capture());
        // La única fuente del protagonista es el cuento (su copia propia); el catálogo ni se consulta
        assertTrue(captor.getValue().protagonist().contains("pelo azul"));
        verify(stories).getStory(STORY);
    }

    @Test
    void contextIsClosedBeforeTheAsyncTaskRuns() {
        Story story = story(StoryStatus.SCRIPT_ACCEPTED, PageStatus.ACCEPTED);
        when(stories.getStory(STORY)).thenReturn(story);
        List<Runnable> tasks = new ArrayList<>();
        StoryPageService deferred = new StoryPageService(mock(OllamaPort.class), "writer", mock(ChatClient.class), pages,
            mock(StoryPageReviewRepository.class), stories, outlines, gate, writer, versions, notifier, tasks::add, mock(StoryNarrationUseCase.class));

        deferred.startPage(STORY, 1);
        story.getCharacters().get(0).setVisualDescription("editado después");
        tasks.get(0).run();

        ArgumentCaptor<WriterContext> captor = ArgumentCaptor.forClass(WriterContext.class);
        verify(writer).writePage(captor.capture());
        assertTrue(captor.getValue().protagonist().contains("pelo azul"));
        assertTrue(!captor.getValue().protagonist().contains("editado después"));
    }

    @Test
    void busyGateRejectsWithoutCallingTheWriter() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, PageStatus.ACCEPTED));
        when(gate.tryAcquire(any(), any())).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> service.startPage(STORY, 1));

        assertTrue(ex.getMessage().contains("generación en curso"));
        verifyNoInteractions(writer, notifier);
        verify(gate, never()).release();
    }

    @Test
    void writerFailurePublishesFailedCreatesNothingAndReleasesTheGate() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, PageStatus.ACCEPTED));
        when(writer.writePage(any())).thenThrow(new AppException("ollama caído", HttpStatus.BAD_REQUEST));

        GenerationStarted started = service.startPage(STORY, 1);

        assertEquals(1, started.pageNumber());
        verify(pages, never()).createPending(anyLong(), anyInt(), any());
        verify(versions, never()).add(anyLong(), any());
        verify(notifier).started(STORY, 1);
        verify(notifier).failed(STORY, 1);
        verify(notifier, never()).versionPresented(anyLong(), anyInt());
        verify(gate).release();
    }

    @Test
    void versionFailureRemovesOnlyThatPageAndPublishesFailed() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, PageStatus.ACCEPTED));
        when(versions.add(eq(70L), any())).thenThrow(new RuntimeException("bd caída"));

        service.startPage(STORY, 1);

        verify(pages).delete(70L);
        verify(pages, never()).deleteByStory(anyLong());
        verify(notifier).failed(STORY, 1);
        verify(gate).release();
    }

    @Test
    void gateIsReleasedWhenTheTaskCannotBeScheduled() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, PageStatus.ACCEPTED));
        StoryPageService rejecting = new StoryPageService(mock(OllamaPort.class), "writer", mock(ChatClient.class), pages,
            mock(StoryPageReviewRepository.class), stories, outlines, gate, writer, versions, notifier,
            task -> { throw new RuntimeException("cola llena"); }, mock(StoryNarrationUseCase.class));

        assertThrows(RuntimeException.class, () -> rejecting.startPage(STORY, 1));

        verify(gate).release();
        verifyNoInteractions(writer);
    }

    // ---- helpers ----

    private static StoryBeat beat(String event) {
        return new StoryBeat(event, "nudo", List.of("Luna", "Conejo"), "curioso", "claro del bosque");
    }

    /** Story con escaleta; {@code statuses[0]} es el de la portada (null = sin portada) y el resto, el de las páginas 1..n. */
    private static Story story(StoryStatus status, PageStatus... statuses) {
        Story story = new Story();
        story.setId(STORY);
        story.setStatus(status);
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
        List<StoryPage> list = new ArrayList<>();
        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i] == null)
                continue;
            StoryPage p = new StoryPage();
            p.setPage(i);
            p.setPageStatus(statuses[i]);
            list.add(p);
        }
        story.setPages(list);
        return story;
    }
}
