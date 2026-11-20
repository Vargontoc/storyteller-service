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
import es.vargontoc.storyteller.domain.response.StoryCoverAgentResult;
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
        service = new StoryPageService(ollama, "writer", client, pages, reviews, stories, outlines);
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
    void coverIsGeneratedWithAcceptedScriptWithoutBeat() {
        when(stories.getStory(STORY)).thenReturn(story(StoryStatus.SCRIPT_ACCEPTED, 0));
        when(client.prompt().user(anyConsumer()).call().entity(StoryCoverAgentResult.class))
            .thenReturn(new StoryCoverAgentResult(new SceneComposition("portada", List.of(), "bosque")));

        StoryPage cover = service.generate(new StoryPageGenerateCommand(STORY));

        assertNotNull(cover);
        verify(pages).create(any(), eq(STORY), eq(0));
        verifyNoInteractions(outlines);
        List<String> params = capturedParams();
        assertTrue(params.contains("synopsis") && params.contains("actors"));
        assertTrue(!params.contains("beat"));
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
