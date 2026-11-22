package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import es.vargontoc.storyteller.application.ports.in.generator.VoiceLibraryUseCase;
import es.vargontoc.storyteller.application.ports.out.GenerationGatePort.GenerationType;
import es.vargontoc.storyteller.application.ports.out.NarrationExecutor;
import es.vargontoc.storyteller.application.ports.out.persistence.NarrationRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.domain.enums.NarrationDisabledReason;
import es.vargontoc.storyteller.domain.enums.NarrationRejectionReason;
import es.vargontoc.storyteller.domain.enums.NarrationStatus;
import es.vargontoc.storyteller.domain.enums.PageStatus;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryBeat;
import es.vargontoc.storyteller.domain.model.StoryNarration;
import es.vargontoc.storyteller.domain.model.StoryOutline;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.model.StoryScriptVersion;
import es.vargontoc.storyteller.domain.response.NarrationState;
import es.vargontoc.storyteller.domain.response.VoiceEntry;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import es.vargontoc.storyteller.shared.exceptions.NarrationRejectedException;

/**
 * TASK-019: estado de narracion calculado desde las paginas interiores y borrado del audio a peticion.
 * TASK-020: inicio de la narracion y la renarracion.
 */
class StoryNarrationServiceTest {

    private static final Long STORY_ID = 5L;
    private static final int BEATS = 8;

    private StoryRepository stories;
    private StoryOutlineRepository outlines;
    private InMemoryNarrations narrations;
    private StoryNarrationService service;
    private Story story;
    private GenerationGate gate;
    private VoiceLibraryUseCase voices;
    private NarrationNotifier notifier;
    private NarrationExecutor executor;

    private static final VoiceEntry SYSTEM = new VoiceEntry(null, "Narradora", "es", true);
    private static final VoiceEntry UPLOADED = new VoiceEntry(9L, "Abuela", "es", false);
    private static final byte[] SAMPLE = { 1, 2, 3 };

    @BeforeEach
    void setUp() {
        stories = mock(StoryRepository.class);
        outlines = mock(StoryOutlineRepository.class);
        narrations = new InMemoryNarrations();
        gate = new GenerationGate();
        voices = mock(VoiceLibraryUseCase.class);
        notifier = mock(NarrationNotifier.class);
        executor = mock(NarrationExecutor.class);
        service = new StoryNarrationService(stories, outlines, narrations, gate, voices, notifier, executor);

        story = new Story();
        story.setId(STORY_ID);
        story.setScriptVersion(StoryScriptVersion.OUTLINE);
        story.setPages(new ArrayList<>());
        when(stories.getStory(STORY_ID)).thenReturn(story);

        List<StoryBeat> beats = new ArrayList<>();
        for (int i = 1; i <= BEATS; i++)
            beats.add(new StoryBeat("Beat " + i, "nudo", List.of("Luna"), "curioso", "claro"));
        when(outlines.find(STORY_ID)).thenReturn(Optional.of(new StoryOutline(List.of(), beats)));
    }

    @Test
    void storyWithoutOutlineDoesNotOfferNarration() {
        story.setScriptVersion(null);

        NarrationState state = service.getState(STORY_ID);

        assertFalse(state.hasOutline());
        assertFalse(state.canNarrate());
        assertEquals(NarrationDisabledReason.NO_OUTLINE, state.disabledReason());
    }

    @Test
    void sevenOfEightAcceptedDisablesNarrationBecausePagesAreMissing() {
        interior(7, PageStatus.ACCEPTED);
        page(8, PageStatus.PENDING);

        NarrationState state = service.getState(STORY_ID);

        assertTrue(state.hasOutline());
        assertFalse(state.canNarrate());
        assertEquals(NarrationDisabledReason.PAGES_NOT_ACCEPTED, state.disabledReason());
    }

    @Test
    void notYetGeneratedInteriorPagesAlsoDisableNarration() {
        interior(7, PageStatus.ACCEPTED);   // la octava aun no existe

        assertEquals(NarrationDisabledReason.PAGES_NOT_ACCEPTED, service.getState(STORY_ID).disabledReason());
    }

    @Test
    void eightAcceptedWithPendingCoverEnablesNarration() {
        page(0, PageStatus.PENDING);
        interior(8, PageStatus.ACCEPTED);

        NarrationState state = service.getState(STORY_ID);

        assertTrue(state.canNarrate());
        assertNull(state.disabledReason());
        assertNull(state.lastResult());
    }

    @Test
    void narrationInProgressIsIndicatedAndDisabled() {
        interior(8, PageStatus.ACCEPTED);
        narrations.markInProgress(STORY_ID);

        NarrationState state = service.getState(STORY_ID);

        assertTrue(state.narrating());
        assertFalse(state.canNarrate());
        assertEquals(NarrationDisabledReason.IN_PROGRESS, state.disabledReason());
        assertEquals(NarrationStatus.IN_PROGRESS, state.lastResult());
        assertTrue(service.isNarrating(STORY_ID));
        assertFalse(service.hasAudio(STORY_ID));
    }

    @Test
    void storyWithAudioReportsTheVoiceUsed() {
        interior(8, PageStatus.ACCEPTED);
        narrations.markInProgress(STORY_ID);
        narrations.publish(STORY_ID, "Abuela", audios());

        NarrationState state = service.getState(STORY_ID);

        assertTrue(state.hasAudio());
        assertEquals("Abuela", state.voiceName());
        assertEquals(NarrationStatus.COMPLETED, state.lastResult());
        assertTrue(state.canNarrate());   // renarrar
        assertFalse(state.narrating());
        assertTrue(service.hasAudio(STORY_ID));
        assertFalse(service.isNarrating(STORY_ID));
    }

    @Test
    void failedAttemptIsReportedAsErrorWithoutAudioNorVoice() {
        interior(8, PageStatus.ACCEPTED);
        narrations.markError(STORY_ID);

        NarrationState state = service.getState(STORY_ID);

        assertEquals(NarrationStatus.ERROR, state.lastResult());
        assertFalse(state.hasAudio());
        assertNull(state.voiceName());
        assertTrue(state.canNarrate());
    }

    @Test
    void deletingAudioLeavesNoAudioNorVoiceAndNarrationFollowsThePagesState() {
        interior(8, PageStatus.ACCEPTED);
        narrations.markInProgress(STORY_ID);
        narrations.publish(STORY_ID, "Abuela", audios());

        // un cambio de páginas reabre la 5 y se pide el borrado
        story.getPages().stream().filter(p -> p.getPage() == 5).forEach(p -> p.setPageStatus(PageStatus.PENDING));
        service.deleteAudio(STORY_ID);

        NarrationState afterDelete = service.getState(STORY_ID);
        assertFalse(afterDelete.hasAudio());
        assertNull(afterDelete.voiceName());
        assertFalse(afterDelete.canNarrate());
        assertEquals(NarrationDisabledReason.PAGES_NOT_ACCEPTED, afterDelete.disabledReason());

        // al aceptar de nuevo todas las páginas vuelve a habilitarse
        story.getPages().stream().filter(p -> p.getPage() == 5).forEach(p -> p.setPageStatus(PageStatus.ACCEPTED));
        NarrationState reaccepted = service.getState(STORY_ID);
        assertTrue(reaccepted.canNarrate());
        assertFalse(reaccepted.hasAudio());
    }

    @Test
    void deletingWhenThereIsNoNarrationIsHarmless() {
        service.deleteAudio(STORY_ID);

        assertFalse(service.hasAudio(STORY_ID));
        assertFalse(service.isNarrating(STORY_ID));
    }

    // ---- TASK-020: inicio de la narracion y la renarracion ----

    @Test
    void startWithConditionsMetAcquiresTurnAndHandsWorkToExecutor() {
        readyToNarrate();
        eligible(SYSTEM, UPLOADED);

        NarrationState state = service.start(STORY_ID, null);   // null = voz del sistema

        assertTrue(state.narrating());
        assertEquals(NarrationStatus.IN_PROGRESS, state.lastResult());
        assertTrue(gate.isGeneratingStory(STORY_ID));
        verify(notifier).started(STORY_ID);
        verify(executor).execute(STORY_ID, "Narradora", SAMPLE);
    }

    @Test
    void startWhileNarratingRejectsOtherGenerationsWithTheUsualNotice() {
        readyToNarrate();
        eligible(SYSTEM);
        service.start(STORY_ID, null);

        assertFalse(gate.tryAcquire(GenerationType.PORTRAITS, null));
    }

    @Test
    void renarrationDeletesPreviousAudioAndVoiceWithSameOrAnotherVoice() {
        readyToNarrate();
        eligible(SYSTEM, UPLOADED);
        narrations.markInProgress(STORY_ID);
        narrations.publish(STORY_ID, "Abuela", audios());

        service.start(STORY_ID, 9L);   // misma voz que la anterior

        assertFalse(service.hasAudio(STORY_ID));
        assertTrue(service.isNarrating(STORY_ID));
        verify(executor).execute(STORY_ID, "Abuela", SAMPLE);

        gate.release();
        narrations.publish(STORY_ID, "Abuela", audios());

        service.start(STORY_ID, null);   // otra voz elegible

        assertFalse(service.hasAudio(STORY_ID));
        verify(executor).execute(STORY_ID, "Narradora", SAMPLE);
    }

    @Test
    void anotherGenerationInProgressRejectsWithoutTouchingAudioNorVoice() {
        readyToNarrate();
        eligible(SYSTEM);
        narrations.markInProgress(STORY_ID);
        narrations.publish(STORY_ID, "Abuela", audios());
        gate.tryAcquire(GenerationType.PORTRAITS, null);

        NarrationRejectedException e = assertRejected(null, NarrationRejectionReason.GENERATION_IN_PROGRESS);

        assertEquals("Hay una generación en curso", e.getMessage());
        assertEquals(409, e.getStatus().value());
        assertTrue(service.hasAudio(STORY_ID));
        assertEquals("Abuela", service.getState(STORY_ID).voiceName());
        verify(notifier, never()).started(anyLong());
        verify(executor, never()).execute(anyLong(), anyString(), any());
    }

    @Test
    void noEligibleVoicesRejectsAndKeepsPreviousAudio() {
        readyToNarrate();
        eligible();
        narrations.markInProgress(STORY_ID);
        narrations.publish(STORY_ID, "Abuela", audios());

        assertRejected(null, NarrationRejectionReason.NO_VOICES_AVAILABLE);

        assertTrue(service.hasAudio(STORY_ID));
        assertFalse(gate.isGeneratingStory(STORY_ID));
    }

    @Test
    void storyWithoutOutlineRejectsWithoutAskingForTheTurn() {
        story.setScriptVersion(null);
        eligible(SYSTEM);

        assertRejected(null, NarrationRejectionReason.NO_OUTLINE);
        assertTurnNeverRequested();
    }

    @Test
    void pagesNotAcceptedRejectsWithoutAskingForTheTurn() {
        interior(7, PageStatus.ACCEPTED);
        eligible(SYSTEM);

        assertRejected(null, NarrationRejectionReason.PAGES_NOT_ACCEPTED);
        assertTurnNeverRequested();
    }

    @Test
    void alreadyNarratingRejectsWithoutAskingForTheTurn() {
        readyToNarrate();
        eligible(SYSTEM);
        narrations.markInProgress(STORY_ID);

        assertRejected(null, NarrationRejectionReason.IN_PROGRESS);
        assertTurnNeverRequested();
    }

    @Test
    void deletedOrNonSpanishVoiceRejectsWithoutAskingForTheTurn() {
        readyToNarrate();
        eligible(SYSTEM);   // la 77 (borrada o en otro idioma) no esta entre las elegibles

        assertRejected(77L, NarrationRejectionReason.VOICE_NOT_AVAILABLE);
        assertTurnNeverRequested();
    }

    @Test
    void voiceDeletedAfterValidationReleasesTheTurnAndKeepsAudio() {
        readyToNarrate();
        eligible(SYSTEM);
        when(voices.eligibleSample(null)).thenReturn(Optional.empty());
        narrations.markInProgress(STORY_ID);
        narrations.publish(STORY_ID, "Abuela", audios());

        assertRejected(null, NarrationRejectionReason.VOICE_NOT_AVAILABLE);

        assertFalse(gate.isGeneratingStory(STORY_ID));
        assertTrue(service.hasAudio(STORY_ID));
    }

    @Test
    void failureHandingOverTheWorkReleasesTheTurn() {
        readyToNarrate();
        eligible(SYSTEM);
        doThrow(new IllegalStateException("boom")).when(executor).execute(anyLong(), anyString(), any());

        assertThrows(IllegalStateException.class, () -> service.start(STORY_ID, null));

        assertFalse(gate.isGeneratingStory(STORY_ID));
    }

    // ---- TASK-022: descarga del audio de una pagina interior ----

    @Test
    void narratedStoryReturnsOnlyTheAudioOfTheRequestedPage() {
        narrations.markInProgress(STORY_ID);
        narrations.publish(STORY_ID, "Abuela", audios());

        assertArrayEquals(new byte[] { 2 }, service.getPageAudio(STORY_ID, 2));
    }

    @Test
    void coverHasNoAudio() {
        narrations.markInProgress(STORY_ID);
        narrations.publish(STORY_ID, "Abuela", audios());

        assertAudioNotFound(0);
    }

    @Test
    void storyWithoutAudioHasNoPageAudio() {
        assertAudioNotFound(2);
    }

    @Test
    void narrationInProgressOrFailedServesNoAudio() {
        narrations.markInProgress(STORY_ID);
        assertAudioNotFound(2);

        narrations.markError(STORY_ID);
        assertAudioNotFound(2);
    }

    @Test
    void renarrationInProgressDoesNotServeThePreviousAudio() {
        readyToNarrate();
        eligible(SYSTEM);
        narrations.markInProgress(STORY_ID);
        narrations.publish(STORY_ID, "Abuela", audios());

        service.start(STORY_ID, null);

        assertAudioNotFound(2);
    }

    @Test
    void audioIsStillServedAfterTheUploadedVoiceIsDeleted() {
        narrations.markInProgress(STORY_ID);
        narrations.publish(STORY_ID, "Abuela", audios());
        eligible(SYSTEM);   // la voz subida "Abuela" ya no existe

        for (int page = 1; page <= BEATS; page++)
            assertArrayEquals(new byte[] { (byte) page }, service.getPageAudio(STORY_ID, page));
        assertEquals("Abuela", service.getState(STORY_ID).voiceName());
    }

    private void assertAudioNotFound(int page) {
        AppException e = assertThrows(AppException.class, () -> service.getPageAudio(STORY_ID, page));
        assertEquals(404, e.getStatus().value());
    }

    private void readyToNarrate() {
        interior(BEATS, PageStatus.ACCEPTED);
    }

    private void eligible(VoiceEntry... entries) {
        when(voices.eligible()).thenReturn(List.of(entries));
        for (VoiceEntry v : entries)
            when(voices.eligibleSample(v.id())).thenReturn(Optional.of(SAMPLE));
    }

    private NarrationRejectedException assertRejected(Long voiceId, NarrationRejectionReason reason) {
        NarrationRejectedException e = assertThrows(NarrationRejectedException.class,
            () -> service.start(STORY_ID, voiceId));
        assertSame(reason, e.getReason());
        return e;
    }

    private void assertTurnNeverRequested() {
        assertFalse(gate.isGeneratingStory(STORY_ID));
        assertTrue(gate.tryAcquire(GenerationType.PORTRAITS, null));   // el turno sigue libre
        verify(notifier, never()).started(anyLong());
        verify(executor, never()).execute(anyLong(), anyString(), any());
    }

    // ---- helpers ----

    private void interior(int count, PageStatus status) {
        for (int i = 1; i <= count; i++)
            page(i, status);
    }

    private void page(int number, PageStatus status) {
        StoryPage p = new StoryPage();
        p.setId((long) number + 100);
        p.setPage(number);
        p.setPageStatus(status);
        story.getPages().add(p);
    }

    private static Map<Integer, byte[]> audios() {
        Map<Integer, byte[]> audios = new HashMap<>();
        for (int page = 1; page <= BEATS; page++)
            audios.put(page, new byte[] { (byte) page });
        return audios;
    }

    /** Repositorio en memoria con la semantica del puerto. */
    private static class InMemoryNarrations implements NarrationRepository {
        private NarrationStatus status;
        private String voice;
        private List<Integer> pages = new ArrayList<>();
        private Map<Integer, byte[]> audios = new HashMap<>();

        @Override
        public StoryNarration markInProgress(Long storyId) {
            status = NarrationStatus.IN_PROGRESS;
            voice = null;
            pages = new ArrayList<>();
            audios = new HashMap<>();
            return current(storyId);
        }

        @Override
        public Optional<StoryNarration> find(Long storyId) {
            return status == null ? Optional.empty() : Optional.of(current(storyId));
        }

        @Override
        public List<StoryNarration> findInProgress() {
            return List.of(); // no lo usa este servicio (es de la reconciliacion)
        }

        @Override
        public void publish(Long storyId, String voiceName, Map<Integer, byte[]> audiosByPage) {
            status = NarrationStatus.COMPLETED;
            voice = voiceName;
            pages = new ArrayList<>(audiosByPage.keySet());
            audios = new HashMap<>(audiosByPage);
        }

        @Override
        public Optional<byte[]> findAudio(Long storyId, int page) {
            return Optional.ofNullable(audios.get(page));
        }

        @Override
        public void markError(Long storyId) {
            status = NarrationStatus.ERROR;
            voice = null;
            pages = new ArrayList<>();
            audios = new HashMap<>();
        }

        @Override
        public void delete(Long storyId) {
            status = null;
            voice = null;
            pages = new ArrayList<>();
            audios = new HashMap<>();
        }

        private StoryNarration current(Long storyId) {
            return new StoryNarration(storyId, status, voice, pages);
        }
    }
}
