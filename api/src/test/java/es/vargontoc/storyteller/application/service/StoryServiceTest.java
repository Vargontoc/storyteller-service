package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.http.HttpStatus;

import es.vargontoc.storyteller.application.ports.out.GenerationGatePort.GenerationType;
import es.vargontoc.storyteller.application.ports.out.external.CharacterImageConditionPort;
import es.vargontoc.storyteller.application.ports.out.external.DirectorAgent;
import es.vargontoc.storyteller.application.ports.out.persistence.CatalogCharacterRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryReviewRepository;
import es.vargontoc.storyteller.domain.command.StoryGenerateCommand;
import es.vargontoc.storyteller.domain.command.StoryReviewCommand;
import es.vargontoc.storyteller.domain.enums.RevisionStatus;
import es.vargontoc.storyteller.domain.enums.StorySize;
import es.vargontoc.storyteller.domain.enums.StoryStatus;
import es.vargontoc.storyteller.domain.model.CatalogCharacter;
import es.vargontoc.storyteller.domain.model.EpisodicCastMember;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryBeat;
import es.vargontoc.storyteller.domain.model.StoryOutline;
import es.vargontoc.storyteller.domain.model.StoryReview;
import es.vargontoc.storyteller.domain.model.StoryScriptVersion;
import es.vargontoc.storyteller.domain.request.DirectorReviewRequest;
import es.vargontoc.storyteller.domain.request.DirectorScriptRequest;
import es.vargontoc.storyteller.domain.response.DirectorBeat;
import es.vargontoc.storyteller.domain.response.DirectorCastMember;
import es.vargontoc.storyteller.domain.response.DirectorReviewResult;
import es.vargontoc.storyteller.domain.response.DirectorScriptResult;
import es.vargontoc.storyteller.domain.response.StoryAgentResult;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto.ConfirmReviewRequestDto;
import es.vargontoc.storyteller.shared.exceptions.AppException;

class StoryServiceTest {

    private static final StorySize SIZE = StorySize.S;

    private StoryRepository stories;
    private StoryReviewRepository reviews;
    private StoryOutlineRepository outlines;
    private DirectorAgent director;
    private CatalogCharacterRepository catalog;
    private CharacterImageConditionPort images;
    private PortraitLockService lock;
    private GenerationGate gate;
    private StoryService service;

    @BeforeEach
    void setUp() {
        stories = mock(StoryRepository.class);
        reviews = mock(StoryReviewRepository.class);
        outlines = mock(StoryOutlineRepository.class);
        director = mock(DirectorAgent.class);
        catalog = mock(CatalogCharacterRepository.class);
        images = mock(CharacterImageConditionPort.class);
        lock = mock(PortraitLockService.class);
        gate = new GenerationGate();
        service = new StoryService(stories, reviews, outlines, director, catalog, images, lock, gate);

        Story created = story(10L, StoryStatus.SCRIPT_PENDING, StoryScriptVersion.OUTLINE);
        when(stories.create(any(), any(), anyString(), anyString(), anyLong())).thenReturn(created);
        when(stories.getStory(10L)).thenReturn(created);
    }

    // --- Creación: protagonista ---

    @Test
    void withoutProtagonistPicksOnlyAmongAvailableOnes() {
        CatalogCharacter withPortrait = luna(7L);
        CatalogCharacter withoutPortrait = new CatalogCharacter(8L, "Draft", "n", "v", "e", List.of());
        CatalogCharacter locked = new CatalogCharacter(9L, "Locked", "n", "v", "e", List.of());
        when(catalog.findAll()).thenReturn(List.of(withoutPortrait, withPortrait, locked));
        when(images.hasApprovedPortrait(7L)).thenReturn(true);
        when(images.hasApprovedPortrait(9L)).thenReturn(true);
        when(lock.isLocked(9L)).thenReturn(true);
        when(director.generateScript(any())).thenReturn(script(SIZE.getPages()));

        service.generate(new StoryGenerateCommand("Amistad", "Un paseo", SIZE));

        verify(stories).create(any(), eq(SIZE), eq("Amistad"), eq("Un paseo"), eq(7L));
    }

    @Test
    void withoutAnyApprovedPortraitIsRejectedAndNothingIsCreated() {
        when(catalog.findAll()).thenReturn(List.of(new CatalogCharacter(8L, "Draft", "n", "v", "e", List.of())));

        assertThrows(AppException.class, () -> service.generate(new StoryGenerateCommand("Amistad", "Un paseo", SIZE)));

        verifyNoInteractions(director, outlines);
        verify(stories, never()).create(any(), any(), anyString(), anyString(), anyLong());
    }

    @Test
    void explicitProtagonistWithoutApprovedPortraitIsRejected() {
        when(catalog.findById(8L)).thenReturn(new CatalogCharacter(8L, "Draft", "n", "v", "e", List.of()));

        assertThrows(AppException.class, () -> service.generate(new StoryGenerateCommand("Amistad", "Un paseo", SIZE, 8L)));

        verifyNoInteractions(director, outlines);
    }

    @Test
    void explicitProtagonistIsSnapshotAndSentToTheDirector() {
        when(catalog.findById(7L)).thenReturn(luna(7L));
        when(images.hasApprovedPortrait(7L)).thenReturn(true);
        when(director.generateScript(any())).thenReturn(script(SIZE.getPages()));

        service.generate(new StoryGenerateCommand("Amistad", "Un paseo", SIZE, 7L));

        ArgumentCaptor<DirectorScriptRequest> request = ArgumentCaptor.forClass(DirectorScriptRequest.class);
        verify(director).generateScript(request.capture());
        assertEquals("Luna", request.getValue().protagonistName());
        assertEquals("Una zorra curiosa", request.getValue().protagonistNarrative());
        assertEquals("Zorra naranja", request.getValue().protagonistVisual());
        assertEquals(7L, request.getValue().protagonistCatalogId());
        assertEquals(SIZE.getPages(), request.getValue().pages());
        assertEquals("Amistad", request.getValue().topic());
        assertEquals("Un paseo", request.getValue().description());

        // La instantánea del protagonista (ficha de catálogo) viaja como único personaje del cuento
        ArgumentCaptor<StoryAgentResult> persisted = ArgumentCaptor.forClass(StoryAgentResult.class);
        verify(stories).create(persisted.capture(), eq(SIZE), eq("Amistad"), eq("Un paseo"), eq(7L));
        assertEquals(1, persisted.getValue().characters().size());
        assertTrue(persisted.getValue().characters().get(0).mainCharacter());
        assertEquals("Luna", persisted.getValue().characters().get(0).name());
    }

    // --- Creación: escaleta ---

    @Test
    void incompleteBeatIsRetriedAndStoryIsNotPersisted() {
        when(catalog.findById(7L)).thenReturn(luna(7L));
        when(images.hasApprovedPortrait(7L)).thenReturn(true);
        DirectorScriptResult incomplete = new DirectorScriptResult("T", "S", List.of(),
                List.of(new DirectorBeat("algo", "", List.of("Luna"), "alegre", "nota")));
        when(director.generateScript(any())).thenReturn(incomplete);

        assertThrows(AppException.class, () -> service.generate(new StoryGenerateCommand("Amistad", "Un paseo", SIZE, 7L)));

        verify(director, times(3)).generateScript(any());
        verify(stories, never()).create(any(), any(), anyString(), anyString(), anyLong());
        verify(outlines, never()).save(anyLong(), any());
    }

    @Test
    void invalidResponseIsRetriedUntilAValidOneArrives() {
        when(catalog.findById(7L)).thenReturn(luna(7L));
        when(images.hasApprovedPortrait(7L)).thenReturn(true);
        when(director.generateScript(any()))
                .thenThrow(new AppException("incompleta", org.springframework.http.HttpStatus.BAD_REQUEST))
                .thenReturn(script(SIZE.getPages()));

        service.generate(new StoryGenerateCommand("Amistad", "Un paseo", SIZE, 7L));

        verify(director, times(2)).generateScript(any());
        verify(outlines).save(eq(10L), any());
    }

    @Test
    void completeScriptIsPersistedWithOutlineAndLightCastOnly() {
        when(catalog.findById(7L)).thenReturn(luna(7L));
        when(images.hasApprovedPortrait(7L)).thenReturn(true);
        when(director.generateScript(any())).thenReturn(script(SIZE.getPages()));

        Story result = service.generate(new StoryGenerateCommand("Amistad", "Un paseo", SIZE, 7L));

        assertEquals(10L, result.getId());
        ArgumentCaptor<StoryOutline> outline = ArgumentCaptor.forClass(StoryOutline.class);
        verify(outlines).save(eq(10L), outline.capture());
        assertEquals(SIZE.getPages(), outline.getValue().beats().size());
        assertEquals(List.of(new EpisodicCastMember("Pipo", "Amigo")), outline.getValue().cast());
        // El único personaje que se da de alta con el cuento es el protagonista (instantánea de catálogo)
        ArgumentCaptor<StoryAgentResult> persisted = ArgumentCaptor.forClass(StoryAgentResult.class);
        verify(stories).create(persisted.capture(), any(), anyString(), anyString(), anyLong());
        assertEquals("T", persisted.getValue().title());
        assertEquals(1, persisted.getValue().characters().size());
    }

    // --- Aprobación ---

    @Test
    void approvingPendingScriptMarksItAccepted() {
        when(stories.getStory(10L)).thenReturn(story(10L, StoryStatus.SCRIPT_PENDING, StoryScriptVersion.OUTLINE));

        service.approve(10L);

        verify(stories).updateStatus(10L, StoryStatus.SCRIPT_ACCEPTED);
    }

    @Test
    void approvingALegacyStoryIsRejected() {
        when(stories.getStory(11L)).thenReturn(story(11L, StoryStatus.SCRIPT_PENDING, null));

        assertThrows(AppException.class, () -> service.approve(11L));

        verify(stories, never()).updateStatus(anyLong(), any());
    }

    // --- Hint global ---

    @Test
    void acceptedHintCreatesPendingReviewWithCandidate() {
        arrangeCurrentOutline();
        DirectorReviewResult accepted = new DirectorReviewResult(true, null, "Nuevo título", "Nueva sinopsis",
                List.of(new DirectorCastMember("Pipo", "Amigo")), beats(SIZE.getPages()));
        when(director.reviewScript(any())).thenReturn(accepted);
        StoryReview pending = new StoryReview();
        when(reviews.createOutlineReview(anyLong(), any(), anyBoolean(), any(), any(), any(), any())).thenReturn(pending);

        StoryReview result = service.review(new StoryReviewCommand(10L, "Final en la playa"));

        assertSame(pending, result);
        ArgumentCaptor<DirectorReviewRequest> request = ArgumentCaptor.forClass(DirectorReviewRequest.class);
        verify(director).reviewScript(request.capture());
        assertEquals(SIZE.getPages(), request.getValue().beats().size());
        assertEquals("Final en la playa", request.getValue().hint());
        verify(reviews).changeStatus(10L, RevisionStatus.DISCARDED);
        ArgumentCaptor<StoryOutline> candidate = ArgumentCaptor.forClass(StoryOutline.class);
        verify(reviews).createOutlineReview(eq(10L), eq("Final en la playa"), eq(true), isNullString(),
                eq("Nuevo título"), eq("Nueva sinopsis"), candidate.capture());
        assertEquals(SIZE.getPages(), candidate.getValue().beats().size());
        verify(outlines, never()).save(anyLong(), any());
    }

    @Test
    void rejectedHintKeepsCurrentOutlineAndStoresReason() {
        arrangeCurrentOutline();
        when(director.reviewScript(any())).thenReturn(new DirectorReviewResult(false, "No apto para 3-4 años", null, null, null, null));

        service.review(new StoryReviewCommand(10L, "Que haya un monstruo"));

        verify(reviews).createOutlineReview(10L, "Que haya un monstruo", false, "No apto para 3-4 años", null, null, null);
        verify(outlines, never()).save(anyLong(), any());
        verify(stories, never()).updateScript(anyLong(), any(), any());
    }

    @Test
    void reviewingALegacyStoryIsRejected() {
        when(stories.getStory(11L)).thenReturn(story(11L, StoryStatus.SCRIPT_PENDING, null));

        assertThrows(AppException.class, () -> service.review(new StoryReviewCommand(11L, "hint")));

        verifyNoInteractions(director);
    }

    @Test
    void confirmingTheCandidateAppliesTitleAndReturnsToPending() {
        StoryReview pending = new StoryReview();
        Story preview = new Story();
        preview.setTitle("Nuevo título");
        preview.setSynopsis("Nueva sinopsis");
        pending.setPreviewStory(preview);
        pending.setCandidateOutline(new StoryOutline(List.of(), List.of()));
        when(reviews.getPendingReview(10L)).thenReturn(pending);

        service.confirmReview(new ConfirmReviewRequestDto(10L, RevisionStatus.CONFIRMED));

        verify(reviews).changeStatus(10L, RevisionStatus.CONFIRMED);
        verify(stories).updateScript(10L, "Nuevo título", "Nueva sinopsis");
    }

    @Test
    void discardingTheCandidateLeavesTheStoryUntouched() {
        when(reviews.getPendingReview(10L)).thenReturn(new StoryReview());

        service.confirmReview(new ConfirmReviewRequestDto(10L, RevisionStatus.DISCARDED));

        verify(reviews).changeStatus(10L, RevisionStatus.DISCARDED);
        verify(stories, never()).updateScript(anyLong(), any(), any());
    }

    // --- Turno de generación ---

    @Test
    void generateWithBusyGateIsRejectedWithoutCallingTheDirectorNorCreatingAnything() {
        when(catalog.findById(7L)).thenReturn(luna(7L));
        when(images.hasApprovedPortrait(7L)).thenReturn(true);
        assertTrue(gate.tryAcquire(GenerationType.PORTRAITS, null));

        AppException e = assertThrows(AppException.class,
                () -> service.generate(new StoryGenerateCommand("Amistad", "Un paseo", SIZE, 7L)));

        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertEquals("Hay una generación en curso", e.getMessage());
        verifyNoInteractions(director, outlines);
        verify(stories, never()).create(any(), any(), anyString(), anyString(), anyLong());
        gate.release(); // la puerta sigue ocupada por quien la tenía: no la ha liberado el rechazo
        assertTrue(gate.tryAcquire(GenerationType.WRITER, null));
    }

    @Test
    void reviewWithBusyGateIsRejectedWithoutCallingTheDirectorNorDiscardingTheReview() {
        arrangeCurrentOutline();
        assertTrue(gate.tryAcquire(GenerationType.WRITER, 10L));

        AppException e = assertThrows(AppException.class, () -> service.review(new StoryReviewCommand(10L, "hint")));

        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertEquals("Hay una generación en curso", e.getMessage());
        verifyNoInteractions(director, reviews);
    }

    @Test
    void whileTheScriptIsGeneratedOtherGenerationsAreRejectedAndGateIsFreedAfterwards() {
        when(catalog.findById(7L)).thenReturn(luna(7L));
        when(images.hasApprovedPortrait(7L)).thenReturn(true);
        boolean[] otherAcquired = {true};
        when(director.generateScript(any())).thenAnswer(inv -> {
            otherAcquired[0] = gate.tryAcquire(GenerationType.CHARACTER_AGENT, null);
            return script(SIZE.getPages());
        });

        service.generate(new StoryGenerateCommand("Amistad", "Un paseo", SIZE, 7L));

        assertTrue(!otherAcquired[0]);
        assertTrue(gate.tryAcquire(GenerationType.WRITER, null));
    }

    @Test
    void whileTheReviewRunsTheStoryIsBusyAndGateIsFreedAfterwards() {
        arrangeCurrentOutline();
        boolean[] busy = {false};
        when(director.reviewScript(any())).thenAnswer(inv -> {
            busy[0] = gate.isGeneratingStory(10L);
            return new DirectorReviewResult(false, "No apto", null, null, null, null);
        });

        service.review(new StoryReviewCommand(10L, "hint"));

        assertTrue(busy[0]);
        assertTrue(gate.tryAcquire(GenerationType.WRITER, null));
    }

    @Test
    void gateIsFreedWhenGenerateOrReviewFail() {
        when(catalog.findById(7L)).thenReturn(luna(7L));
        when(images.hasApprovedPortrait(7L)).thenReturn(true);
        when(director.generateScript(any())).thenThrow(new RuntimeException("caído"));
        assertThrows(AppException.class, () -> service.generate(new StoryGenerateCommand("Amistad", "Un paseo", SIZE, 7L)));
        assertTrue(gate.tryAcquire(GenerationType.WRITER, null));
        gate.release();

        arrangeCurrentOutline();
        when(director.reviewScript(any())).thenThrow(new RuntimeException("caído"));
        assertThrows(AppException.class, () -> service.review(new StoryReviewCommand(10L, "hint")));
        assertTrue(gate.tryAcquire(GenerationType.WRITER, null));
    }

    // --- Utilidades ---

    private void arrangeCurrentOutline() {
        when(outlines.find(10L)).thenReturn(java.util.Optional.of(
                new StoryOutline(List.of(new EpisodicCastMember("Pipo", "Amigo")), storyBeats(SIZE.getPages()))));
    }

    private static String isNullString() {
        return org.mockito.ArgumentMatchers.isNull();
    }

    private static CatalogCharacter luna(Long id) {
        return new CatalogCharacter(id, "Luna", "Una zorra curiosa", "Zorra naranja", "orange fox", List.of("orange"));
    }

    private static Story story(Long id, StoryStatus status, Integer version) {
        Story story = new Story();
        story.setId(id);
        story.setTitle("Título");
        story.setSynopsis("Sinopsis");
        story.setStatus(status);
        story.setScriptVersion(version);
        return story;
    }

    private static DirectorScriptResult script(int pages) {
        return new DirectorScriptResult("T", "S", List.of(new DirectorCastMember("Pipo", "Amigo")), beats(pages));
    }

    private static List<DirectorBeat> beats(int pages) {
        return java.util.stream.IntStream.range(0, pages)
                .mapToObj(i -> new DirectorBeat("Pasa " + i, "inicio", List.of("Luna"), "alegre", "bosque"))
                .toList();
    }

    private static List<StoryBeat> storyBeats(int pages) {
        return java.util.stream.IntStream.range(0, pages)
                .mapToObj(i -> new StoryBeat("Pasa " + i, "inicio", List.of("Luna"), "alegre", "bosque"))
                .toList();
    }
}
