package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;

import es.vargontoc.storyteller.application.ports.out.GenerationGatePort;
import es.vargontoc.storyteller.application.ports.out.GenerationGatePort.GenerationType;
import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageReviewRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.domain.command.StoryPageGenerateCommand;
import es.vargontoc.storyteller.domain.enums.StorySize;
import es.vargontoc.storyteller.domain.enums.StoryStatus;
import es.vargontoc.storyteller.domain.enums.RevisionStatus;
import es.vargontoc.storyteller.domain.model.ActorAction;
import es.vargontoc.storyteller.domain.model.StoryPageReview;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto.ConfirmReviewRequestDto;
import es.vargontoc.storyteller.domain.model.SceneComposition;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryBeat;
import es.vargontoc.storyteller.domain.model.StoryOutline;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.model.StoryScriptVersion;
import es.vargontoc.storyteller.application.ports.out.external.WriterAgent;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageVersionRepository;
import es.vargontoc.storyteller.domain.enums.PageLengthWarning;
import es.vargontoc.storyteller.domain.model.PageVersion;
import es.vargontoc.storyteller.domain.request.WriterContext;
import es.vargontoc.storyteller.domain.response.GenerationStarted;
import es.vargontoc.storyteller.domain.response.WriterResult;
import es.vargontoc.storyteller.domain.response.StoryPageAgentResult;
import es.vargontoc.storyteller.shared.exceptions.AppException;

class StoryPageServiceTest {

    private static final Long STORY = 5L;

    private OllamaPort ollama;
    private ChatClient client;
    private StoryPageRepository pages;
    private StoryRepository stories;
    private StoryOutlineRepository outlines;
    private StoryPageReviewRepository reviews;
    private GenerationGatePort gate;
    private WriterAgent writer;
    private StoryPageVersionRepository versions;
    private StoryPageNotifier notifier;
    private StoryPageService service;

    private final StoryOutline outline = new StoryOutline(List.of(),
        List.of(beat("Luna sale"), beat("Luna encuentra una piedra"), beat("Luna vuelve")));

    @BeforeEach
    void setUp() {
        ollama = mock(OllamaPort.class);
        client = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        pages = mock(StoryPageRepository.class);
        stories = mock(StoryRepository.class);
        outlines = mock(StoryOutlineRepository.class);
        reviews = mock(StoryPageReviewRepository.class);
        gate = mock(GenerationGatePort.class);
        when(gate.tryAcquire(any(), any())).thenReturn(true);
        writer = mock(WriterAgent.class);
        versions = mock(StoryPageVersionRepository.class);
        notifier = mock(StoryPageNotifier.class);
        // Ejecutor síncrono: la tarea asíncrona corre dentro de la propia llamada
        service = new StoryPageService(ollama, "writer", client, pages, reviews, stories, outlines, gate,
            writer, versions, notifier, Runnable::run);
        when(ollama.isAvailable("writer")).thenReturn(true);
        when(outlines.find(STORY)).thenReturn(Optional.of(outline));
        when(pages.create(any(), anyLong(), anyInt())).thenReturn(new StoryPage());
    }

    @Test
    void coverIsRejectedWhileScriptIsPending() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_PENDING, 0));

        AppException ex = assertThrows(AppException.class, () -> service.generate(new StoryPageGenerateCommand(STORY)));

        assertTrue(ex.getMessage().contains("aprobarse el guion"));
        verifyNoInteractions(client);
        verify(pages, never()).create(any(), anyLong(), anyInt());
    }

    @Test
    void interiorPageIsRejectedWhileScriptIsPending() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_PENDING, 1));

        AppException ex = assertThrows(AppException.class, () -> service.generate(new StoryPageGenerateCommand(STORY)));

        assertTrue(ex.getMessage().contains("aprobarse el guion"));
        verifyNoInteractions(client);
    }

    @Test
    void generateOfAnOutlineStoryWithoutCoverPointsToTheCoverOperation() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, 0));

        AppException ex = assertThrows(AppException.class, () -> service.generate(new StoryPageGenerateCommand(STORY)));

        assertTrue(ex.getMessage().contains("portada"));
        verifyNoInteractions(client, gate);
        verify(pages, never()).create(any(), anyLong(), anyInt());
    }

    // ---- TASK-011: portada con escaleta ----

    @Test
    void coverStartsCreatesPendingCoverWithVersionAndPublishesEvents() {
        Story story = story(StoryStatus.SCRIPT_ACCEPTED, 0);
        story.setTitle("El bosque de Luna");
        when(stories.getStory(STORY)).thenReturn(story);
        SceneComposition scene = new SceneComposition("Luna en el claro", List.of(), "claro");
        when(writer.writeCover(any())).thenReturn(new WriterResult(null, scene));
        StoryPage cover = new StoryPage();
        cover.setId(40L);
        when(pages.createPending(STORY, 0, null)).thenReturn(cover);

        GenerationStarted started = service.startCover(STORY);

        assertEquals(new GenerationStarted(STORY, 0), started);
        verify(gate).tryAcquire(GenerationType.WRITER, STORY);
        ArgumentCaptor<WriterContext> context = ArgumentCaptor.forClass(WriterContext.class);
        verify(writer).writeCover(context.capture());
        assertTrue(context.getValue().isCover());
        assertEquals("El bosque de Luna", context.getValue().title());
        assertEquals("sinopsis", context.getValue().synopsis());
        ArgumentCaptor<PageVersion> version = ArgumentCaptor.forClass(PageVersion.class);
        verify(versions).add(eq(40L), version.capture());
        assertEquals(scene, version.getValue().getScene());
        assertEquals(null, version.getValue().getText());
        assertEquals(PageLengthWarning.NONE, version.getValue().getWarning());
        assertTrue(version.getValue().isCurrent());
        verify(notifier).started(STORY, 0);
        verify(notifier).versionPresented(STORY, 0);
        verify(notifier, never()).failed(anyLong(), anyInt());
        verify(gate).release();
        verifyNoInteractions(client);
    }

    @Test
    void coverWithLongSceneIsSavedWithoutWarning() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, 0));
        String longScene = "palabra ".repeat(80);
        when(writer.writeCover(any())).thenReturn(new WriterResult(null, new SceneComposition(longScene, List.of(), "claro")));
        StoryPage cover = new StoryPage();
        cover.setId(40L);
        when(pages.createPending(STORY, 0, null)).thenReturn(cover);

        service.startCover(STORY);

        ArgumentCaptor<PageVersion> version = ArgumentCaptor.forClass(PageVersion.class);
        verify(versions).add(eq(40L), version.capture());
        assertEquals(PageLengthWarning.NONE, version.getValue().getWarning());
    }

    @Test
    void coverIsRejectedWhenScriptIsNotApprovedWithoutTakingTheTurn() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_PENDING, 0));

        AppException ex = assertThrows(AppException.class, () -> service.startCover(STORY));

        assertTrue(ex.getMessage().contains("aprobarse el guion"));
        verifyNoInteractions(gate, writer, notifier);
    }

    @Test
    void coverIsRejectedWhenTheStoryAlreadyHasOne() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, 0, true));

        AppException ex = assertThrows(AppException.class, () -> service.startCover(STORY));

        assertTrue(ex.getMessage().contains("Ya hay una portada"));
        verifyNoInteractions(gate, writer, notifier);
    }

    @Test
    void coverIsRejectedForAnOldStory() {
        when(stories.getStory(STORY)).thenReturn(oldStory(0));

        assertThrows(AppException.class, () -> service.startCover(STORY));

        verifyNoInteractions(gate, writer, notifier);
    }

    @Test
    void coverIsRejectedWhileTheGateIsBusy() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, 0));
        when(gate.tryAcquire(any(), any())).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> service.startCover(STORY));

        assertTrue(ex.getMessage().contains("generación en curso"));
        verifyNoInteractions(writer, notifier);
        verify(gate, never()).release();
    }

    @Test
    void coverWriterFailurePublishesFailedCreatesNothingAndReleasesTheGate() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, 0));
        when(writer.writeCover(any())).thenThrow(new AppException("ollama caído", org.springframework.http.HttpStatus.BAD_REQUEST));

        GenerationStarted started = service.startCover(STORY);

        assertEquals(0, started.pageNumber());
        verify(pages, never()).createPending(anyLong(), anyInt(), any());
        verify(versions, never()).add(anyLong(), any());
        verify(notifier).started(STORY, 0);
        verify(notifier).failed(STORY, 0);
        verify(notifier, never()).versionPresented(anyLong(), anyInt());
        verify(gate).release();
    }

    @Test
    void coverVersionFailureRemovesThePendingCoverAndPublishesFailed() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, 0));
        when(writer.writeCover(any())).thenReturn(new WriterResult(null, new SceneComposition("escena", List.of(), "claro")));
        StoryPage cover = new StoryPage();
        cover.setId(40L);
        when(pages.createPending(STORY, 0, null)).thenReturn(cover);
        when(versions.add(eq(40L), any())).thenThrow(new RuntimeException("bd caída"));

        service.startCover(STORY);

        verify(pages).deleteByStory(STORY);
        verify(notifier).failed(STORY, 0);
        verify(gate).release();
    }

    @Test
    void coverReleasesTheGateWhenTheTaskCannotBeScheduled() {
        StoryPageService rejecting = new StoryPageService(ollama, "writer", client, pages, reviews, stories, outlines,
            gate, writer, versions, notifier, task -> { throw new RuntimeException("cola llena"); });
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, 0));

        assertThrows(RuntimeException.class, () -> rejecting.startCover(STORY));

        verify(gate).release();
        verifyNoInteractions(writer);
    }

    @Test
    void interiorPageReceivesTheBeatOfItsNumber() {
        // Portada + una pagina interior ya generadas: toca la pagina 2
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, 1));
        when(client.prompt().user(anyConsumer()).call().entity(StoryPageAgentResult.class))
            .thenReturn(new StoryPageAgentResult("texto", new SceneComposition("escena", List.of(), "bosque"), "resumen"));

        service.generate(new StoryPageGenerateCommand(STORY));

        verify(pages).create(any(), eq(STORY), eq(2));
        String beat = capturedBeat();
        assertTrue(beat.contains("Luna encuentra una piedra"));
        assertTrue(beat.contains("nudo") && beat.contains("Luna, Bruno") && beat.contains("curioso")
            && beat.contains("claro del bosque"));
        assertTrue(!beat.contains("Luna sale") && !beat.contains("Luna vuelve"));
    }

    @Test
    void firstInteriorPageReceivesFirstBeat() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, 0, true));
        when(client.prompt().user(anyConsumer()).call().entity(StoryPageAgentResult.class))
            .thenReturn(new StoryPageAgentResult("texto", new SceneComposition("escena", List.of(), "bosque"), "resumen"));

        service.generate(new StoryPageGenerateCommand(STORY));

        assertTrue(capturedBeat().contains("Luna sale"));
    }

    @Test
    void oldStoryWithoutOutlineKeepsLegacyFlow() {
        Story old = story(StoryStatus.SCRIPT_PENDING, 1);
        old.setScriptVersion(null);
        when(stories.getStory(STORY)).thenReturn(old);
        when(client.prompt().user(anyConsumer()).call().entity(StoryPageAgentResult.class))
            .thenReturn(new StoryPageAgentResult("texto", new SceneComposition("escena", List.of(), "bosque"), "resumen"));

        service.generate(new StoryPageGenerateCommand(STORY));

        verifyNoInteractions(outlines);
        verify(pages).create(any(), eq(STORY), eq(2));
    }

    @Test
    void oldStoryGeneratesInsideTheGateAndReleasesIt() {
        when(stories.getStory(STORY)).thenReturn(oldStory(1));
        when(client.prompt().user(anyConsumer()).call().entity(StoryPageAgentResult.class))
            .thenReturn(new StoryPageAgentResult("texto", new SceneComposition("escena", List.of(), "bosque"), "resumen"));

        service.generate(new StoryPageGenerateCommand(STORY));

        verify(gate).tryAcquire(GenerationType.WRITER, STORY);
        verify(gate).release();
        verify(pages).create(any(), eq(STORY), eq(2));
    }

    @Test
    void oldStoryIsRejectedWhileGateIsBusy() {
        when(stories.getStory(STORY)).thenReturn(oldStory(1));
        when(gate.tryAcquire(any(), any())).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> service.generate(new StoryPageGenerateCommand(STORY)));

        assertTrue(ex.getMessage().contains("generación en curso"));
        verifyNoInteractions(client);
        verify(pages, never()).create(any(), anyLong(), anyInt());
        verify(gate, never()).release();
    }

    @Test
    void oldStoryReleasesTheGateWhenGenerationFails() {
        when(stories.getStory(STORY)).thenReturn(oldStory(1));
        when(client.prompt().user(anyConsumer()).call().entity(StoryPageAgentResult.class))
            .thenThrow(new RuntimeException("ollama caído"));

        assertThrows(RuntimeException.class, () -> service.generate(new StoryPageGenerateCommand(STORY)));

        verify(gate).release();
    }

    @Test
    void outlineStoryDoesNotUseTheLegacyReview() {
        StoryPage page = new StoryPage();
        page.setStoryId(STORY);
        page.setPage(1);
        when(pages.getPage(9L)).thenReturn(page);
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, 1));

        assertThrows(AppException.class, () -> service.review(
            new es.vargontoc.storyteller.domain.command.StoryPageReviewCommand(9L, "hint", null)));

        verifyNoInteractions(reviews, client, gate);
    }

    @Test
    void oldStoryReviewDiscardsThePreviousOneInsideTheGate() {
        StoryPage page = new StoryPage();
        page.setStoryId(STORY);
        page.setPage(0);
        when(pages.getPage(9L)).thenReturn(page);
        Story old = oldStory(1);
        when(stories.getStory(STORY)).thenReturn(old);
        when(client.prompt().user(anyConsumer()).call().entity(es.vargontoc.storyteller.domain.response.StoryCoverReviewAgentResult.class))
            .thenReturn(new es.vargontoc.storyteller.domain.response.StoryCoverReviewAgentResult(
                new SceneComposition("escena", List.of(), "bosque"), true, null));

        service.review(new es.vargontoc.storyteller.domain.command.StoryPageReviewCommand(9L, "hint", null));

        verify(reviews).changeStatus(9L, RevisionStatus.DISCARDED);
        verify(gate).tryAcquire(GenerationType.WRITER, STORY);
        verify(gate).release();
    }

    @Test
    void oldStoryReviewIsRejectedWhileGateIsBusyWithoutDiscarding() {
        StoryPage page = new StoryPage();
        page.setStoryId(STORY);
        when(pages.getPage(9L)).thenReturn(page);
        when(stories.getStory(STORY)).thenReturn(oldStory(1));
        when(gate.tryAcquire(any(), any())).thenReturn(false);

        assertThrows(AppException.class, () -> service.review(
            new es.vargontoc.storyteller.domain.command.StoryPageReviewCommand(9L, "hint", null)));

        verifyNoInteractions(reviews, client);
    }

    @Test
    void generatingAPageWithSecondaryActorsDoesNotCreateCharacters() {
        Story story = story(StoryStatus.SCRIPT_ACCEPTED, 1);
        when(stories.getStory(STORY)).thenReturn(story);
        SceneComposition scene = new SceneComposition("escena", List.of(new ActorAction("Bruno", "mira")), "bosque");
        when(client.prompt().user(anyConsumer()).call().entity(StoryPageAgentResult.class))
            .thenReturn(new StoryPageAgentResult("texto", scene, "resumen"));

        service.generate(new StoryPageGenerateCommand(STORY));

        verify(pages).create(any(), eq(STORY), eq(2));
        assertTrue(story.getCharacters().isEmpty());
        verify(stories).getStory(STORY);
        verifyNoMoreInteractions(stories);
    }

    @Test
    void confirmingAPageReviewDoesNotCreateCharacters() {
        StoryPage current = new StoryPage();
        current.setId(7L);
        StoryPage updated = new StoryPage();
        updated.setComposition(new SceneComposition("escena", List.of(new ActorAction("Bruno", "mira")), "bosque"));
        StoryPageReview review = new StoryPageReview();
        when(pages.getPage(7L)).thenReturn(current);
        when(reviews.getPendingReview(7L)).thenReturn(review);
        when(pages.update(7L, review)).thenReturn(updated);

        StoryPage result = service.confirmReview(new ConfirmReviewRequestDto(7L, RevisionStatus.CONFIRMED));

        assertEquals(updated, result);
        verify(pages).update(7L, review);
        verifyNoInteractions(stories);
    }

    @Test
    void deleteInvalidatedPagesReturnsRemovedCount() {
        when(pages.deleteByStory(STORY)).thenReturn(3);

        assertEquals(3, service.deleteInvalidatedPages(STORY));
        verify(pages).deleteByStory(STORY);
    }

    // ---- helpers ----

    @SuppressWarnings("unchecked")
    private static Consumer<ChatClient.PromptUserSpec> anyConsumer() {
        return any(Consumer.class);
    }

    /** Ejecuta la lambda del prompt contra un spec simulado y devuelve los nombres de parametros enviados. */
    @SuppressWarnings("unchecked")
    private List<String> capturedParams() {
        return runPrompt().stream().map(a -> (String) a[0]).toList();
    }

    private String capturedBeat() {
        return runPrompt().stream().filter(a -> "beat".equals(a[0])).map(a -> (String) a[1]).findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> runPrompt() {
        ArgumentCaptor<Consumer<ChatClient.PromptUserSpec>> captor = ArgumentCaptor.forClass(Consumer.class);
        // atLeastOnce: el stubbing de la prueba tambien cuenta como invocacion; getValue() devuelve la ultima (la real)
        verify(client.prompt(), org.mockito.Mockito.atLeastOnce()).user(captor.capture());
        ChatClient.PromptUserSpec spec = mock(ChatClient.PromptUserSpec.class, Answers.RETURNS_SELF);
        captor.getValue().accept(spec);
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> value = ArgumentCaptor.forClass(Object.class);
        verify(spec, org.mockito.Mockito.atLeast(0)).param(key.capture(), value.capture());
        List<Object[]> out = new ArrayList<>();
        for (int i = 0; i < key.getAllValues().size(); i++)
            out.add(new Object[] { key.getAllValues().get(i), String.valueOf(value.getAllValues().get(i)) });
        return out;
    }

    private static Story oldStory(int interior) {
        Story old = story(StoryStatus.SCRIPT_PENDING, interior);
        old.setScriptVersion(null);
        return old;
    }

    private static StoryBeat beat(String event) {
        return new StoryBeat(event, "nudo", List.of("Luna", "Bruno"), "curioso", "claro del bosque");
    }

    /** Story con portada + {@code interior} paginas interiores. */
    private static Story story(StoryStatus status, int interior) {
        return story(status, interior, false);
    }

    private static Story story(StoryStatus status, int interior, boolean coverOnly) {
        Story story = new Story();
        story.setId(STORY);
        story.setStatus(status);
        story.setScriptVersion(StoryScriptVersion.OUTLINE);
        story.setSize(StorySize.S);
        story.setSynopsis("sinopsis");
        story.setAgentSummary("");
        story.setCharacters(List.of());
        List<StoryPage> list = new ArrayList<>();
        if (interior > 0 || coverOnly) {
            StoryPage cover = new StoryPage();
            cover.setPage(0);
            list.add(cover);
        }
        for (int i = 1; i <= interior; i++) {
            StoryPage p = new StoryPage();
            p.setPage(i);
            list.add(p);
        }
        story.setPages(list);
        return story;
    }
}
