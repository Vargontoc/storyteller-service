package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import es.vargontoc.storyteller.application.ports.in.generator.StoryNarrationUseCase;
import es.vargontoc.storyteller.application.ports.out.GenerationGatePort;
import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageVersionRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.domain.enums.PageAction;
import es.vargontoc.storyteller.domain.enums.PageLengthWarning;
import es.vargontoc.storyteller.domain.enums.PageStatus;
import es.vargontoc.storyteller.domain.enums.StorySize;
import es.vargontoc.storyteller.domain.enums.StoryStatus;
import es.vargontoc.storyteller.domain.model.PageVersion;
import es.vargontoc.storyteller.domain.model.SceneComposition;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryBeat;
import es.vargontoc.storyteller.domain.model.StoryOutline;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.model.StoryScriptVersion;
import es.vargontoc.storyteller.domain.response.PageDetail;
import es.vargontoc.storyteller.domain.response.StoryPagesView;
import es.vargontoc.storyteller.shared.exceptions.AppException;

/** TASK-022: consulta de portada y páginas con acciones calculadas e imagen de la versión aceptada. */
class StoryPageQueryServiceTest {

    private static final Long STORY = 5L;
    private static final int BEATS = 8;

    private StoryRepository stories;
    private StoryPageVersionRepository versions;
    private StoryOutlineRepository outlines;
    private GenerationGatePort gate;
    private ResourceStorage storage;
    private StoryPageQueryService service;
    private StoryNarrationUseCase narration;

    private final SceneComposition scene = new SceneComposition("Luna en el claro", List.of(), "claro");
    private final List<StoryPage> pages = new ArrayList<>();
    private StoryStatus status = StoryStatus.SCRIPT_ACCEPTED;
    private Integer scriptVersion = StoryScriptVersion.OUTLINE;

    @BeforeEach
    void setUp() {
        stories = mock(StoryRepository.class);
        versions = mock(StoryPageVersionRepository.class);
        outlines = mock(StoryOutlineRepository.class);
        gate = mock(GenerationGatePort.class);
        storage = mock(ResourceStorage.class);
        narration = mock(StoryNarrationUseCase.class);
        service = new StoryPageQueryService(stories, versions, outlines, gate, storage, narration);
        List<StoryBeat> beats = new ArrayList<>();
        for (int i = 1; i <= BEATS; i++)
            beats.add(new StoryBeat("Beat " + i, "nudo", List.of("Luna"), "curioso", "claro"));
        when(outlines.find(STORY)).thenReturn(Optional.of(new StoryOutline(List.of(), beats)));
    }

    // ---- matriz de acciones ----

    @Test
    void pendingWithoutWarningOffersAcceptAndHint() {
        pending(1, version(1, PageLengthWarning.NONE, true));

        assertEquals(List.of(PageAction.ACCEPT, PageAction.HINT), only().actions());
    }

    @Test
    void pendingWithWordsWarningStillOffersAcceptAndHintWithTheWarning() {
        pending(1, version(1, PageLengthWarning.TOO_MANY_WORDS, true));

        PageDetail detail = only();

        assertEquals(List.of(PageAction.ACCEPT, PageAction.HINT), detail.actions());
        assertEquals(PageLengthWarning.TOO_MANY_WORDS, detail.versions().get(0).warning());
    }

    @Test
    void pendingWithSentenceWarningOffersHintAndRegenerateButNotAccept() {
        pending(1, version(1, PageLengthWarning.SENTENCE_COUNT, true));

        assertEquals(List.of(PageAction.HINT, PageAction.REGENERATE), only().actions());
    }

    @Test
    void bothWarningsAlsoBlockAcceptAndAllowRegenerate() {
        pending(1, version(1, PageLengthWarning.BOTH, true));

        assertEquals(List.of(PageAction.HINT, PageAction.REGENERATE), only().actions());
    }

    @Test
    void currentWithoutSentenceWarningDoesNotOfferRegenerateEvenIfAnotherVersionHasIt() {
        pending(1, version(1, PageLengthWarning.SENTENCE_COUNT, false), version(2, PageLengthWarning.NONE, true));

        List<PageAction> actions = only().actions();

        assertFalse(actions.contains(PageAction.REGENERATE));
        assertTrue(actions.contains(PageAction.ACCEPT));
    }

    @Test
    void pendingWithSeveralVersionsReturnsAllWithWarningAndTheCurrentOne() {
        pending(1, version(1, PageLengthWarning.TOO_MANY_WORDS, false), version(2, PageLengthWarning.SENTENCE_COUNT, false),
            version(3, PageLengthWarning.TOO_MANY_WORDS, true));

        PageDetail detail = only();

        assertEquals(3, detail.versions().size());
        assertEquals(List.of(false, false, true), detail.versions().stream().map(v -> v.current()).toList());
        assertEquals(PageLengthWarning.SENTENCE_COUNT, detail.versions().get(1).warning());
        assertTrue(detail.actions().contains(PageAction.SELECT_VERSION));
        assertTrue(detail.proposals().isEmpty());
        assertNull(detail.accepted());
    }

    @Test
    void pendingWithASingleVersionDoesNotOfferSelectVersion() {
        pending(1, version(1, PageLengthWarning.NONE, true));

        assertFalse(only().actions().contains(PageAction.SELECT_VERSION));
    }

    @Test
    void elevenVersionsAreAllReturned() {
        PageVersion[] history = new PageVersion[11];
        for (int i = 0; i < 11; i++)
            history[i] = version(i + 1, PageLengthWarning.NONE, i == 10);
        pending(1, history);

        assertEquals(11, only().versions().size());
    }

    @Test
    void acceptedWithoutProposalsOffersOnlyHintAndHasNoHistory() {
        accepted(1, null);

        PageDetail detail = only();

        assertEquals(PageStatus.ACCEPTED, detail.status());
        assertEquals(List.of(PageAction.HINT), detail.actions());
        assertTrue(detail.versions().isEmpty());
        assertTrue(detail.proposals().isEmpty());
    }

    @Test
    void interiorPageWithTextAndNoStatusOffersAcceptAndHint() {
        StoryPage p = page(2, null, null);
        p.setText("Texto antiguo.");
        pages.add(p);
        when(versions.findByPage(p.getId())).thenReturn(List.of());

        assertEquals(List.of(PageAction.ACCEPT, PageAction.HINT), only().actions());
    }

    @Test
    void pageWithoutStatusAndWithoutTextDoesNotOfferAccept() {
        pages.add(page(2, null, null));
        when(versions.findByPage(102L)).thenReturn(List.of());

        assertEquals(List.of(PageAction.HINT), only().actions());
    }

    // ---- reapertura ----

    @Test
    void reopenedPageSeparatesTheAcceptedVersionFromTheProposals() {
        accepted(1, "imagenes/p1.png", version(1, PageLengthWarning.NONE, false), version(2, PageLengthWarning.TOO_MANY_WORDS, true));

        PageDetail detail = only();

        assertEquals("Texto aceptado 1.", detail.accepted().text());
        assertEquals("Luna en el claro", detail.accepted().scene());
        assertEquals(2, detail.proposals().size());
        assertTrue(detail.versions().isEmpty());
        assertEquals(List.of(PageAction.HINT, PageAction.SELECT_VERSION, PageAction.ACCEPT_PROPOSAL, PageAction.DISCARD_PROPOSALS),
            detail.actions());
    }

    @Test
    void proposalWithSentenceWarningCanBeDiscardedButNotAccepted() {
        accepted(1, null, version(1, PageLengthWarning.SENTENCE_COUNT, true));

        List<PageAction> actions = only().actions();

        assertFalse(actions.contains(PageAction.ACCEPT_PROPOSAL));
        assertTrue(actions.contains(PageAction.DISCARD_PROPOSALS));
        assertTrue(actions.contains(PageAction.HINT));
    }

    @Test
    void reopenedCoverHasTitleAndNoText() {
        cover(true, "portadas/c.png", new PageVersion(null, scene, PageLengthWarning.NONE, true));

        PageDetail detail = only();

        assertEquals("El conejo blanco", detail.title());
        assertNull(detail.accepted().text());
        assertTrue(detail.accepted().hasImage());
        assertEquals(1, detail.proposals().size());
        assertNull(detail.proposals().get(0).text());
    }

    // ---- siguiente página y cuento completo ----

    @Test
    void storyWithoutPagesOffersTheCover() {
        assertEquals(0, view().nextPage());
    }

    @Test
    void lastExistingPageAcceptedAndNotLastOfTheOutlineOffersTheNextOne() {
        cover(true, null);
        accepted(1, null);
        accepted(2, null);

        assertEquals(3, view().nextPage());
    }

    @Test
    void pendingPageOrCoverDoesNotOfferGeneratingMore() {
        cover(true, null);
        pending(1, version(1, PageLengthWarning.NONE, true));
        assertNull(view().nextPage());
    }

    @Test
    void pendingCoverDoesNotOfferGeneratingAPage() {
        pages.add(page(0, PageStatus.PENDING, null));
        when(versions.findByPage(100L)).thenReturn(List.of(version(1, PageLengthWarning.NONE, true)));

        assertNull(view().nextPage());
    }

    @Test
    void completeStoryDoesNotOfferGeneratingMore() {
        status = StoryStatus.PAGES_ACCEPTED;
        cover(true, null);
        for (int i = 1; i <= BEATS; i++)
            accepted(i, null);

        assertNull(view().nextPage());
    }

    @Test
    void lastBeatAcceptedWithScriptStatusStillOffersNothingBeyondTheOutline() {
        cover(true, null);
        for (int i = 1; i <= BEATS; i++)
            accepted(i, null);

        assertNull(view().nextPage());
    }

    @Test
    void storyWithoutApprovedScriptOffersNothing() {
        status = StoryStatus.SCRIPT_PENDING;

        assertNull(view().nextPage());
    }

    // ---- generación en curso ----

    @Test
    void generationInProgressOfTheStoryRemovesEveryActionAndTheNextPage() {
        cover(true, null);
        accepted(1, null);
        pending(2, version(1, PageLengthWarning.NONE, true));
        when(gate.isGeneratingStory(STORY)).thenReturn(true);

        StoryPagesView view = view();

        assertTrue(view.generating());
        assertNull(view.nextPage());
        assertTrue(view.pages().stream().allMatch(p -> p.actions().isEmpty()));
        assertEquals(1, view.pages().get(2).versions().size());
    }

    @Test
    void generationOfAnotherStoryDoesNotRemoveActions() {
        pending(1, version(1, PageLengthWarning.NONE, true));
        when(gate.isGeneratingStory(STORY)).thenReturn(false);

        assertFalse(view().generating());
        assertFalse(only().actions().isEmpty());
    }

    // ---- imagen ----

    @Test
    void acceptedPageWithImageReportsItAndTheImageCanBeRead() {
        accepted(1, "imagenes/p1.png");
        byte[] bytes = {1, 2, 3};
        when(storage.getResource("imagenes/p1.png")).thenReturn(bytes);

        assertTrue(only().accepted().hasImage());
        assertArrayEquals(bytes, service.getAcceptedImage(STORY, 101L));
    }

    @Test
    void acceptedPageWithoutImageReportsItAndTheImageIsNotFound() {
        accepted(1, null);

        assertFalse(only().accepted().hasImage());
        AppException ex = assertThrows(AppException.class, () -> service.getAcceptedImage(STORY, 101L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verifyNoInteractions(storage);
    }

    @Test
    void imageFileMissingInStorageIsNotFound() {
        accepted(1, "imagenes/p1.png");
        story();
        when(storage.getResource("imagenes/p1.png")).thenReturn(null);

        assertEquals(HttpStatus.NOT_FOUND, assertThrows(AppException.class, () -> service.getAcceptedImage(STORY, 101L)).getStatus());
    }

    @Test
    void pendingPageNeverHasImageNorVersionImage() {
        pending(1, version(1, PageLengthWarning.NONE, true));

        assertNull(only().accepted());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(AppException.class, () -> service.getAcceptedImage(STORY, 101L)).getStatus());
    }

    @Test
    void acceptedCoverAndReopenedCoverKeepTheImageOnlyInTheAcceptedContent() {
        cover(true, "portadas/c.png", version(1, PageLengthWarning.NONE, true));
        when(storage.getResource("portadas/c.png")).thenReturn(new byte[] {9});

        PageDetail detail = only();

        assertTrue(detail.accepted().hasImage());
        assertNotNull(service.getAcceptedImage(STORY, 100L));
    }

    @Test
    void pageOfAnotherStoryIsNotFound() {
        accepted(1, "imagenes/p1.png");
        story();

        assertEquals(HttpStatus.NOT_FOUND, assertThrows(AppException.class, () -> service.findPage(STORY, 999L)).getStatus());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(AppException.class, () -> service.getAcceptedImage(STORY, 999L)).getStatus());
    }

    // ---- audio y narración (TASK-026) ----

    @Test
    void narratedStoryReportsAudioInTheViewAndInEveryInteriorPageButNotTheCover() {
        cover(true, null);
        accepted(1, null);
        accepted(2, null);
        when(narration.hasAudio(STORY)).thenReturn(true);

        StoryPagesView view = view();

        assertTrue(view.hasAudio());
        assertFalse(view.narrating());
        assertFalse(view.pages().get(0).hasAudio());
        assertTrue(view.pages().get(1).hasAudio());
        assertTrue(view.pages().get(2).hasAudio());
        assertTrue(service.findPage(STORY, 102L).orElseThrow().hasAudio());
        assertFalse(service.findPage(STORY, 100L).orElseThrow().hasAudio());
    }

    @Test
    void storyBeingNarratedReportsNarratingWithoutAudio() {
        cover(true, null);
        accepted(1, null);
        when(narration.isNarrating(STORY)).thenReturn(true);

        StoryPagesView view = view();

        assertTrue(view.narrating());
        assertFalse(view.hasAudio());
        assertTrue(view.pages().stream().noneMatch(PageDetail::hasAudio));
    }

    @Test
    void storyNeverNarratedHasNoAudioNorNarrating() {
        accepted(1, null);

        StoryPagesView view = view();

        assertFalse(view.hasAudio());
        assertFalse(view.narrating());
        assertFalse(view.pages().get(0).hasAudio());
    }

    // ---- cuentos antiguos ----

    @Test
    void legacyStoryIsNotHandledSoItKeepsItsCurrentResponse() {
        scriptVersion = null;
        accepted(1, null);
        story();

        assertTrue(service.findPages(STORY).isEmpty());
        assertTrue(service.findPage(STORY, 101L).isEmpty());
    }

    // ---- helpers ----

    private PageDetail only() {
        story();
        return service.findPages(STORY).orElseThrow().pages().get(0);
    }

    private StoryPagesView view() {
        story();
        return service.findPages(STORY).orElseThrow();
    }

    private void story() {
        Story story = new Story();
        story.setId(STORY);
        story.setTitle("El conejo blanco");
        story.setStatus(status);
        story.setScriptVersion(scriptVersion);
        story.setSize(StorySize.S);
        story.setPages(pages);
        when(stories.getStory(STORY)).thenReturn(story);
    }

    private void pending(int number, PageVersion... history) {
        StoryPage p = page(number, PageStatus.PENDING, null);
        pages.add(p);
        when(versions.findByPage(p.getId())).thenReturn(List.of(history));
    }

    private void accepted(int number, String image, PageVersion... proposals) {
        StoryPage p = page(number, PageStatus.ACCEPTED, image);
        p.setText("Texto aceptado " + number + ".");
        p.setComposition(scene);
        pages.add(p);
        when(versions.findByPage(p.getId())).thenReturn(List.of(proposals));
    }

    private void cover(boolean accepted, String image, PageVersion... proposals) {
        StoryPage p = page(0, accepted ? PageStatus.ACCEPTED : PageStatus.PENDING, image);
        p.setComposition(scene);
        pages.add(p);
        when(versions.findByPage(p.getId())).thenReturn(List.of(proposals));
    }

    /** El id de la fila es 100 + el número de página: la portada es la 100 y la página 1, la 101. */
    private StoryPage page(int number, PageStatus pageStatus, String image) {
        StoryPage p = new StoryPage();
        p.setId(100L + number);
        p.setStoryId(STORY);
        p.setPage(number);
        p.setPageStatus(pageStatus);
        p.setCoverText("El conejo blanco");
        p.setBeatId(number == 0 ? null : 500L + number);
        p.setImportedImage(image);
        return p;
    }

    private PageVersion version(long id, PageLengthWarning warning, boolean current) {
        PageVersion v = new PageVersion("Texto " + id + ".", scene, warning, current);
        v.setId(id);
        return v;
    }
}
