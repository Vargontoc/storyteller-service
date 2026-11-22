package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import es.vargontoc.storyteller.application.ports.in.persistence.StorytellerUseCase;
import es.vargontoc.storyteller.application.ports.out.GenerationGatePort;
import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageVersionRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.application.service.StoryPageQueryService;
import es.vargontoc.storyteller.domain.enums.PageLengthWarning;
import es.vargontoc.storyteller.domain.enums.PageStatus;
import es.vargontoc.storyteller.domain.enums.StoryStatus;
import es.vargontoc.storyteller.domain.model.PageSummary;
import es.vargontoc.storyteller.domain.model.PageVersion;
import es.vargontoc.storyteller.domain.model.SceneComposition;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryBeat;
import es.vargontoc.storyteller.domain.model.StoryOutline;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.model.StoryScriptVersion;
import es.vargontoc.storyteller.shared.exceptions.GlobalExceptionHandler;

/** TASK-022: forma del JSON de la consulta de páginas, imagen aceptada y ausencia de edición manual. */
class StorytellerControllerPagesQueryTest {

    private static final Long STORY = 5L;

    private StoryRepository stories;
    private StoryPageVersionRepository versions;
    private ResourceStorage storage;
    private StorytellerUseCase useCase;
    private MockMvc mvc;

    private final SceneComposition scene = new SceneComposition("Luna en el claro", List.of(), "claro");

    @BeforeEach
    void setUp() {
        stories = mock(StoryRepository.class);
        versions = mock(StoryPageVersionRepository.class);
        storage = mock(ResourceStorage.class);
        useCase = mock(StorytellerUseCase.class);
        StoryOutlineRepository outlines = mock(StoryOutlineRepository.class);
        when(outlines.find(STORY)).thenReturn(Optional.of(new StoryOutline(List.of(),
            List.of(new StoryBeat("Beat 1", "nudo", List.of("Luna"), "curioso", "claro"),
                    new StoryBeat("Beat 2", "nudo", List.of("Luna"), "curioso", "claro")))));
        var query = new StoryPageQueryService(stories, versions, outlines, mock(GenerationGatePort.class), storage);
        var controller = new StorytellerController(useCase, null, null, null, query);
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void outlineStoryReturnsStatePagesWithAcceptedContentVersionsAndActions() throws Exception {
        // Portada aceptada con imagen y reabierta (una propuesta), página 1 pendiente con dos versiones
        StoryPage cover = page(100L, 0, PageStatus.ACCEPTED, "imagenes/c.png");
        StoryPage page1 = page(101L, 1, PageStatus.PENDING, null);
        withOutline(cover, page1);
        when(versions.findByPage(100L)).thenReturn(List.of(version(1L, null, PageLengthWarning.NONE, true)));
        when(versions.findByPage(101L)).thenReturn(List.of(
            version(2L, "Primera.", PageLengthWarning.SENTENCE_COUNT, false), version(3L, "Segunda.", PageLengthWarning.NONE, true)));

        mvc.perform(get("/api/v1/storyteller/5/pages"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("SCRIPT_ACCEPTED"))
            .andExpect(jsonPath("$.data.generating").value(false))
            .andExpect(jsonPath("$.data.nextPage").doesNotExist())
            .andExpect(jsonPath("$.data.pages.length()").value(2))
            // portada reabierta: aceptada con imagen, propuesta aparte sin imagen y acciones de reapertura
            .andExpect(jsonPath("$.data.pages[0].page").value(0))
            .andExpect(jsonPath("$.data.pages[0].status").value("ACCEPTED"))
            .andExpect(jsonPath("$.data.pages[0].title").value("El conejo blanco"))
            .andExpect(jsonPath("$.data.pages[0].accepted.hasImage").value(true))
            .andExpect(jsonPath("$.data.pages[0].accepted.text").doesNotExist())
            .andExpect(jsonPath("$.data.pages[0].versions.length()").value(0))
            .andExpect(jsonPath("$.data.pages[0].proposals.length()").value(1))
            .andExpect(jsonPath("$.data.pages[0].proposals[0].image").doesNotExist())
            .andExpect(jsonPath("$.data.pages[0].actions[0]").value("HINT"))
            .andExpect(jsonPath("$.data.pages[0].actions[1]").value("ACCEPT_PROPOSAL"))
            .andExpect(jsonPath("$.data.pages[0].actions[2]").value("DISCARD_PROPOSALS"))
            // página pendiente: beat, versiones con aviso y vigente, sin contenido aceptado
            .andExpect(jsonPath("$.data.pages[1].beatId").value(501))
            .andExpect(jsonPath("$.data.pages[1].status").value("PENDING"))
            .andExpect(jsonPath("$.data.pages[1].accepted").doesNotExist())
            .andExpect(jsonPath("$.data.pages[1].versions.length()").value(2))
            .andExpect(jsonPath("$.data.pages[1].versions[0].warning").value("SENTENCE_COUNT"))
            .andExpect(jsonPath("$.data.pages[1].versions[1].current").value(true))
            .andExpect(jsonPath("$.data.pages[1].versions[1].image").doesNotExist())
            .andExpect(jsonPath("$.data.pages[1].proposals.length()").value(0))
            .andExpect(jsonPath("$.data.pages[1].actions[0]").value("ACCEPT"))
            .andExpect(jsonPath("$.data.pages[1].actions[1]").value("HINT"))
            .andExpect(jsonPath("$.data.pages[1].actions[2]").value("SELECT_VERSION"));
    }

    @Test
    void nextPageIsReportedWhenEverythingExistingIsAccepted() throws Exception {
        withOutline(page(100L, 0, PageStatus.ACCEPTED, null), page(101L, 1, PageStatus.ACCEPTED, null));
        when(versions.findByPage(100L)).thenReturn(List.of());
        when(versions.findByPage(101L)).thenReturn(List.of());

        mvc.perform(get("/api/v1/storyteller/5/pages"))
            .andExpect(jsonPath("$.data.nextPage").value(2))
            .andExpect(jsonPath("$.data.pages[1].accepted.hasImage").value(false))
            .andExpect(jsonPath("$.data.pages[1].actions[0]").value("HINT"))
            .andExpect(jsonPath("$.data.pages[1].actions.length()").value(1));
    }

    @Test
    void singlePageReturnsItsDetailAndANotOwnedPageIs404() throws Exception {
        withOutline(page(101L, 1, PageStatus.PENDING, null));
        when(versions.findByPage(101L)).thenReturn(List.of(version(2L, "Texto.", PageLengthWarning.NONE, true)));

        mvc.perform(get("/api/v1/storyteller/5/pages/101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(101))
            .andExpect(jsonPath("$.data.actions[0]").value("ACCEPT"));
        mvc.perform(get("/api/v1/storyteller/5/pages/999")).andExpect(status().isNotFound());
    }

    @Test
    void legacyStoryKeepsItsCurrentResponse() throws Exception {
        Story legacy = new Story();
        legacy.setId(STORY);
        legacy.setScriptVersion(null);
        legacy.setPages(List.of());
        when(stories.getStory(STORY)).thenReturn(legacy);
        when(useCase.getPages(STORY)).thenReturn(List.of(new PageSummary(1L, 0, "Titulo", "Escena", "img.png", null)));
        when(useCase.getPage(STORY, 1L)).thenReturn(new PageSummary(1L, 0, "Titulo", "Escena", "img.png", null));

        mvc.perform(get("/api/v1/storyteller/5/pages"))
            .andExpect(jsonPath("$.data[0].page").value(0))
            .andExpect(jsonPath("$.data[0].image").value("img.png"))
            .andExpect(jsonPath("$.data[0].actions").doesNotExist());
        mvc.perform(get("/api/v1/storyteller/5/pages/1"))
            .andExpect(jsonPath("$.data.text").value("Titulo"))
            .andExpect(jsonPath("$.data.actions").doesNotExist());
        verify(useCase).getPages(STORY);
    }

    @Test
    void acceptedImageIsDownloadedAndAMissingOneIs404() throws Exception {
        withOutline(page(101L, 1, PageStatus.ACCEPTED, "imagenes/p1.png"), page(102L, 2, PageStatus.ACCEPTED, null));
        when(storage.getResource("imagenes/p1.png")).thenReturn(new byte[] {1, 2, 3});

        mvc.perform(get("/api/v1/storyteller/5/pages/101/image"))
            .andExpect(status().isOk())
            .andExpect(content().bytes(new byte[] {1, 2, 3}));
        mvc.perform(get("/api/v1/storyteller/5/pages/102/image")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/storyteller/5/pages/999/image")).andExpect(status().isNotFound());
    }

    @Test
    void thereIsNoOperationToEditTextOrSceneByHand() {
        for (Method m : StorytellerController.class.getDeclaredMethods()) {
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(m, RequestMapping.class);
            if (mapping == null)
                continue;
            for (RequestMethod method : mapping.method())
                assertFalse(method == RequestMethod.PUT || method == RequestMethod.PATCH, m.getName());
            for (String path : mapping.path())
                assertFalse(path.endsWith("/text") || path.endsWith("/scene") || path.endsWith("/edit"), path);
        }
    }

    // ---- helpers ----

    private void withOutline(StoryPage... list) {
        Story story = new Story();
        story.setId(STORY);
        story.setTitle("El conejo blanco");
        story.setStatus(StoryStatus.SCRIPT_ACCEPTED);
        story.setScriptVersion(StoryScriptVersion.OUTLINE);
        story.setPages(new ArrayList<>(List.of(list)));
        when(stories.getStory(STORY)).thenReturn(story);
    }

    private StoryPage page(Long id, int number, PageStatus status, String image) {
        StoryPage p = new StoryPage();
        p.setId(id);
        p.setStoryId(STORY);
        p.setPage(number);
        p.setPageStatus(status);
        p.setCoverText("El conejo blanco");
        p.setBeatId(number == 0 ? null : 500L + number);
        p.setImportedImage(image);
        if (status == PageStatus.ACCEPTED) {
            p.setText(number == 0 ? null : "Texto aceptado.");
            p.setComposition(scene);
        }
        return p;
    }

    private PageVersion version(Long id, String text, PageLengthWarning warning, boolean current) {
        PageVersion v = new PageVersion(text, scene, warning, current);
        v.setId(id);
        return v;
    }
}
