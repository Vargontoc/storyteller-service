package es.vargontoc.storyteller.application.service;

import es.vargontoc.storyteller.application.ports.in.generator.StoryNarrationUseCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import es.vargontoc.storyteller.application.ports.in.character.PortraitSelectionUseCase;
import es.vargontoc.storyteller.application.ports.out.PortraitCandidateAssetStorage;
import es.vargontoc.storyteller.application.ports.out.PortraitGenerationTaskRegistry;
import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.application.ports.out.external.CharacterAgent;
import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.application.ports.out.external.PortraitGeneratorPort;
import es.vargontoc.storyteller.application.ports.out.external.WriterAgent;
import es.vargontoc.storyteller.application.ports.out.persistence.CatalogCharacterRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.PortraitCandidateStore;
import es.vargontoc.storyteller.application.ports.out.persistence.PortraitGenerationJobStore;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageReviewRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageVersionRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.domain.enums.PageStatus;
import es.vargontoc.storyteller.domain.enums.PortraitGenerationStatus;
import es.vargontoc.storyteller.domain.enums.StorySize;
import es.vargontoc.storyteller.domain.enums.StoryStatus;
import es.vargontoc.storyteller.domain.model.Actor;
import es.vargontoc.storyteller.domain.model.CatalogCharacter;
import es.vargontoc.storyteller.domain.model.PortraitGenerationJob;
import es.vargontoc.storyteller.domain.model.SceneComposition;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryBeat;
import es.vargontoc.storyteller.domain.model.StoryOutline;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.model.StoryScriptVersion;
import es.vargontoc.storyteller.domain.response.GenerationStarted;
import es.vargontoc.storyteller.domain.response.WriterResult;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaRepository;
import es.vargontoc.storyteller.infrastructure.validations.CatalogCharacterAgentResultValidator;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;

/** TASK-028: el turno de la puerta y el bloqueo por personaje conviven durante los retratos. */
class PortraitTurnTest {

    private static final Long STORY = 5L;
    private static final Long CHARACTER = 7L;

    @Test
    void pagesAreRejectedAndRenameIsLockedWhilePortraitsRunAndBothFreeWhenTheyEnd() {
        var gate = new GenerationGate();
        var job = new PortraitGenerationJob(15L, CHARACTER, PortraitGenerationStatus.RUNNING, null);
        var jobs = mock(PortraitGenerationJobStore.class);
        when(jobs.start(CHARACTER)).thenReturn(job);
        when(jobs.findRunningByCatalogCharacterId(CHARACTER)).thenReturn(Optional.of(job));
        var characters = mock(CatalogCharacterJpaRepository.class);
        var entity = new CatalogCharacterJpaEntity();
        entity.setVisualDescription("zorra");
        when(characters.findById(CHARACTER)).thenReturn(Optional.of(entity));
        var generator = mock(PortraitGeneratorPort.class);
        when(generator.generatePortraits(any()))
                .thenReturn(List.of(new byte[] {1}, new byte[] {2}, new byte[] {3}, new byte[] {4}));
        var assets = mock(PortraitCandidateAssetStorage.class);
        when(assets.saveCandidateAsset(eq(CHARACTER), any(byte[].class))).thenReturn("a.png");
        List<Runnable> queued = new ArrayList<>();
        var generation = new PortraitGenerationService(jobs, mock(PortraitCandidateStore.class), generator, assets,
                mock(PortraitGenerationTaskRegistry.class), mock(PortraitWebsocketNotifier.class), characters,
                queued::add, gate);
        var catalogRepository = mock(CatalogCharacterRepository.class);
        when(catalogRepository.rename(CHARACTER, "otra")).thenReturn(mock(CatalogCharacter.class));
        var catalog = new CharacterCatalogService(mock(CharacterAgent.class), catalogRepository,
                new CatalogCharacterAgentResultValidator(), id -> false, Optional.empty(), new PortraitLockService(jobs),
                gate, generation, mock(PortraitSelectionUseCase.class), mock(PortraitCandidateStore.class),
                mock(ResourceStorage.class));
        var writer = mock(WriterAgent.class);
        var pages = mock(StoryPageRepository.class);
        var pageService = pageService(gate, writer, pages, story(true));
        var coverService = pageService(gate, writer, pages, story(false));

        catalog.generatePortraits(CHARACTER);

        AppException cover = assertThrows(AppException.class, () -> coverService.startCover(STORY));
        AppException page = assertThrows(AppException.class, () -> pageService.startPage(STORY, 1));
        AppException rename = assertThrows(AppException.class, () -> catalog.rename(CHARACTER, "otra"));
        assertEquals(HttpStatus.CONFLICT, cover.getStatus());
        assertEquals("Hay una generación en curso", cover.getMessage());
        assertEquals(HttpStatus.CONFLICT, page.getStatus());
        assertEquals("Hay una generación en curso", page.getMessage());
        assertEquals(HttpStatus.CONFLICT, rename.getStatus());
        verifyNoInteractions(writer);
        verify(catalogRepository, never()).rename(any(), any());

        // El trabajo termina: el job deja de estar en ejecución y la puerta queda libre
        when(jobs.findRunningByCatalogCharacterId(CHARACTER)).thenReturn(Optional.empty());
        queued.get(0).run();

        assertEquals(new GenerationStarted(STORY, 1), pageService.startPage(STORY, 1));
        verify(writer).writePage(any());
        catalog.rename(CHARACTER, "otra");
        verify(catalogRepository).rename(CHARACTER, "otra");
    }

    private StoryPageService pageService(GenerationGate gate, WriterAgent writer, StoryPageRepository pages, Story story) {
        var stories = mock(StoryRepository.class);
        var outlines = mock(StoryOutlineRepository.class);
        when(stories.getStory(STORY)).thenReturn(story);
        var beat = new StoryBeat("Luna sale", "nudo", List.of("Luna"), "curioso", "claro");
        when(outlines.find(STORY)).thenReturn(Optional.of(new StoryOutline(List.of(), List.of(beat), new es.vargontoc.storyteller.domain.model.CoverPlan("habitación", "calma"))));
        when(outlines.findBeatId(STORY, 1)).thenReturn(Optional.of(501L));
        var created = new StoryPage();
        created.setId(70L);
        when(pages.createPending(eq(STORY), anyInt(), any())).thenReturn(created);
        when(writer.writePage(any())).thenReturn(new WriterResult("Luna sale. Luna corre.",
                new SceneComposition("Luna en el claro", List.of(), "claro")));
        return new StoryPageService(mock(OllamaPort.class), "writer", mock(ChatClient.class), pages,
                mock(StoryPageReviewRepository.class), stories, outlines, gate, writer,
                mock(StoryPageVersionRepository.class), mock(StoryPageNotifier.class), Runnable::run, mock(StoryNarrationUseCase.class), mock(ResourceStorage.class), new ImageContentInspector());
    }

    /** Cuento con guion aprobado; con portada aceptada solo falta la página 1, sin ella falta la portada. */
    private static Story story(boolean withCover) {
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
        StoryPage cover = new StoryPage();
        cover.setPage(0);
        cover.setPageStatus(PageStatus.ACCEPTED);
        story.setPages(withCover ? List.of(cover) : List.of());
        return story;
    }
}
