package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import es.vargontoc.storyteller.application.ports.out.GenerationGatePort.GenerationType;
import es.vargontoc.storyteller.application.ports.out.external.NarratorAgent;
import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.application.ports.out.external.SpeechSynthesizer;
import es.vargontoc.storyteller.application.ports.out.persistence.NarrationRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.application.utils.AudioAssembler;
import es.vargontoc.storyteller.domain.enums.NarrationStatus;
import es.vargontoc.storyteller.domain.model.AudioFragment;
import es.vargontoc.storyteller.domain.model.PageAudioScript;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryBeat;
import es.vargontoc.storyteller.domain.model.StoryNarration;
import es.vargontoc.storyteller.domain.model.StoryOutline;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.request.SpeechSynthesisRequest;
import es.vargontoc.storyteller.shared.exceptions.AppException;

/** TASK-021: ejecucion de la narracion en segundo plano (dobles de narrador, sintetizador y repositorio). */
class BackgroundNarrationExecutorTest {

    private static final Long STORY_ID = 5L;
    private static final int BEATS = 8;
    private static final byte[] SAMPLE = { 1, 2, 3 };

    private final GenerationGate gate = new GenerationGate();
    private final NarrationNotifier notifier = mock(NarrationNotifier.class);
    private final OllamaPort ollama = mock(OllamaPort.class);
    private final InMemoryNarrations narrations = new InMemoryNarrations();
    private final List<SpeechSynthesisRequest> requests = new ArrayList<>();
    private final List<Runnable> submitted = new ArrayList<>();
    private final List<String> texts = new ArrayList<>();

    private int narratorCalls;
    private int synthCalls;
    private AppException narratorFailure;
    private Integer failOnSynthCall;       // 1-based
    private boolean alterWords;
    private BackgroundNarrationExecutor executor;

    @BeforeEach
    void setUp() {
        Story story = new Story();
        story.setId(STORY_ID);
        story.setPages(new ArrayList<>());
        story.getPages().add(page(0, "Portada del cuento"));
        for (int i = 1; i <= BEATS; i++) {
            String text = "Pagina " + i + " del cuento de la luna.";
            story.getPages().add(page(i, text));
            texts.add(text);
        }
        StoryRepository stories = mock(StoryRepository.class);
        when(stories.getStory(STORY_ID)).thenReturn(story);
        StoryOutlineRepository outlines = mock(StoryOutlineRepository.class);
        List<StoryBeat> beats = new ArrayList<>();
        for (int i = 1; i <= BEATS; i++)
            beats.add(new StoryBeat("Beat " + i, "nudo", List.of("Luna"), "curioso", "claro"));
        when(outlines.find(STORY_ID)).thenReturn(Optional.of(new StoryOutline(List.of(), beats)));

        NarratorAgent narrator = pageTexts -> {
            narratorCalls++;
            if (narratorFailure != null)
                throw narratorFailure;
            return script(pageTexts);
        };
        SpeechSynthesizer synthesizer = request -> {
            synthCalls++;
            // Durante la sintesis el cuento esta en curso y sin audio publicado.
            assertEquals(NarrationStatus.IN_PROGRESS, narrations.find(STORY_ID).orElseThrow().getStatus());
            assertTrue(narrations.audios.isEmpty());
            requests.add(request);
            if (failOnSynthCall != null && synthCalls == failOnSynthCall)
                throw new AppException("Chatterbox caido", HttpStatus.BAD_GATEWAY);
            return AudioAssembler.silence(Duration.ofMillis(100));
        };
        TaskExecutor taskExecutor = submitted::add;
        executor = new BackgroundNarrationExecutor(taskExecutor, stories, outlines, narrator, synthesizer,
            ollama, narrations, notifier, gate);

        // Estado que deja StoryNarrationService.start: turno retenido y cuento en curso.
        assertTrue(gate.tryAcquire(GenerationType.NARRATION, STORY_ID));
        narrations.markInProgress(STORY_ID);
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive())
            TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void allCorrectPublishesEightAudiosAndFreesTheGate() {
        executor.narrate(STORY_ID, "Abuela", SAMPLE);

        assertEquals(1, narrations.publishCalls);
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8), new ArrayList<>(narrations.audios.keySet().stream().sorted().toList()));
        assertFalse(narrations.audios.containsKey(0));
        StoryNarration saved = narrations.find(STORY_ID).orElseThrow();
        assertEquals(NarrationStatus.COMPLETED, saved.getStatus());
        assertEquals("Abuela", saved.getVoiceName());
        verify(notifier).completed(STORY_ID, "Abuela");
        verify(notifier, never()).failed(STORY_ID);
        verify(ollama, times(1)).stopAllServices();
        assertGateFree();
    }

    @Test
    void everyFragmentIsSynthesizedInSpanishWithTheChosenSampleAndScriptExpressiveness() {
        executor.narrate(STORY_ID, "Abuela", SAMPLE);

        assertEquals(BEATS * 2, requests.size());          // dos fragmentos por pagina
        for (int i = 0; i < requests.size(); i++) {
            SpeechSynthesisRequest r = requests.get(i);
            assertEquals("es", r.language());
            assertSame(SAMPLE, r.referenceSample());
            assertEquals(0.5 + (i % 2), r.exaggeration());  // fragmento 1: 0.5, fragmento 2: 1.5
            assertEquals(0.4, r.cfgWeight());
            assertEquals(0.8, r.temperature());
        }
        // Se envian los textos de todas las paginas, en orden, y ninguno es la portada.
        String sent = String.join(" ", requests.stream().map(SpeechSynthesisRequest::text).toList());
        assertTrue(sent.startsWith("Pagina 1"));
        assertFalse(sent.contains("Portada"));
    }

    @Test
    void acceptedTextIsIdenticalBeforeAndAfter() {
        List<String> before = new ArrayList<>(texts);

        executor.narrate(STORY_ID, "Abuela", SAMPLE);

        assertEquals(before, texts);
        assertEquals(before, sentToNarrator);
    }

    private List<String> sentToNarrator = new ArrayList<>();

    @Test
    void narratorFailureEndsWithErrorAndNoAudio() {
        narratorFailure = new AppException("Ollama caido", HttpStatus.BAD_GATEWAY);

        executor.narrate(STORY_ID, "Abuela", SAMPLE);

        assertFailed();
        assertEquals(0, synthCalls);
        verify(ollama, never()).stopAllServices();
        assertEquals(1, narratorCalls);   // sin reintento
    }

    @Test
    void validatorDetectingAlteredWordsEndsWithErrorWithoutSynthesizing() {
        alterWords = true;

        executor.narrate(STORY_ID, "Abuela", SAMPLE);

        assertFailed();
        assertEquals(0, synthCalls);
        verify(ollama, never()).stopAllServices();
    }

    @Test
    void synthesisFailingOnPageFourLeavesNoAudioAndAllowsNarratingAgain() {
        failOnSynthCall = 7;   // primer fragmento de la pagina 4 (2 fragmentos por pagina)

        executor.narrate(STORY_ID, "Abuela", SAMPLE);

        assertFailed();
        assertEquals(7, synthCalls);   // sin reintento
        assertEquals(0, narrations.publishCalls);

        // Se puede volver a narrar: el turno esta libre y la siguiente narracion publica.
        failOnSynthCall = null;
        assertTrue(gate.tryAcquire(GenerationType.NARRATION, STORY_ID));
        narrations.markInProgress(STORY_ID);
        executor.narrate(STORY_ID, "Abuela", SAMPLE);
        assertEquals(NarrationStatus.COMPLETED, narrations.find(STORY_ID).orElseThrow().getStatus());
        assertEquals(BEATS, narrations.audios.size());
        assertGateFree();
    }

    @Test
    void failedRenarrationLeavesNoAudioNorVoice() {
        executor.narrate(STORY_ID, "Abuela", SAMPLE);
        assertEquals(BEATS, narrations.audios.size());
        org.mockito.Mockito.clearInvocations(notifier);

        // Renarrar: start descarta el audio anterior al empezar.
        assertTrue(gate.tryAcquire(GenerationType.NARRATION, STORY_ID));
        narrations.markInProgress(STORY_ID);
        failOnSynthCall = synthCalls + 3;
        executor.narrate(STORY_ID, "Otra", SAMPLE);

        assertFailed();
    }

    @Test
    void narrationKeepsTheSampleAndNameReceivedAtStart() {
        byte[] received = { 9, 9, 9 };

        executor.narrate(STORY_ID, "Abuela", received);   // aunque la voz subida se borre, no se vuelve a consultar

        assertTrue(requests.stream().allMatch(r -> r.referenceSample() == received));
        assertEquals("Abuela", narrations.find(STORY_ID).orElseThrow().getVoiceName());
    }

    @Test
    void executeLaunchesTheJobOnTheConfiguredExecutorWithoutRunningItInline() {
        executor.execute(STORY_ID, "Abuela", SAMPLE);

        assertEquals(1, submitted.size());
        assertEquals(0, narratorCalls);
        submitted.get(0).run();
        assertEquals(NarrationStatus.COMPLETED, narrations.find(STORY_ID).orElseThrow().getStatus());
        assertGateFree();
    }

    @Test
    void insideATransactionTheJobIsLaunchedOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();

        executor.execute(STORY_ID, "Abuela", SAMPLE);

        assertTrue(submitted.isEmpty());
        List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
        syncs.forEach(TransactionSynchronization::afterCommit);
        syncs.forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        assertEquals(1, submitted.size());
        assertFalse(gate.tryAcquire(GenerationType.PORTRAITS, 1L));   // el turno sigue retenido hasta que termine
    }

    @Test
    void ifTheTransactionRollsBackNothingRunsAndTheGateIsFreed() {
        TransactionSynchronizationManager.initSynchronization();

        executor.execute(STORY_ID, "Abuela", SAMPLE);
        TransactionSynchronizationManager.getSynchronizations()
            .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertTrue(submitted.isEmpty());
        assertGateFree();
    }

    @Test
    void ifTheJobCannotBeLaunchedTheNarrationFailsAndFreesTheGate() {
        BackgroundNarrationExecutor rejecting = new BackgroundNarrationExecutor(
            job -> { throw new RejectedExecutionException("pool lleno"); },
            mock(StoryRepository.class), mock(StoryOutlineRepository.class), p -> List.of(), r -> new byte[0],
            ollama, narrations, notifier, gate);

        rejecting.execute(STORY_ID, "Abuela", SAMPLE);

        assertFailed();
    }

    private void assertFailed() {
        assertTrue(narrations.audios.isEmpty());
        StoryNarration saved = narrations.find(STORY_ID).orElseThrow();
        assertEquals(NarrationStatus.ERROR, saved.getStatus());
        assertNull(saved.getVoiceName());
        assertFalse(saved.hasAudio());
        verify(notifier).failed(STORY_ID);
        verify(notifier, never()).completed(STORY_ID, "Abuela");
        assertGateFree();
    }

    private void assertGateFree() {
        assertTrue(gate.tryAcquire(GenerationType.PORTRAITS, 1L));
        gate.release();
    }

    private static StoryPage page(int number, String text) {
        StoryPage p = new StoryPage();
        p.setId(100L + number);
        p.setPage(number);
        p.setText(text);
        return p;
    }

    /** Guion del doble: cada pagina en dos fragmentos (mitad de palabras), expresividad distinta por fragmento. */
    private List<PageAudioScript> script(List<String> pageTexts) {
        sentToNarrator = new ArrayList<>(pageTexts);
        List<PageAudioScript> script = new ArrayList<>();
        for (int i = 0; i < pageTexts.size(); i++) {
            String[] words = pageTexts.get(i).split(" ");
            int half = words.length / 2;
            String first = String.join(" ", java.util.Arrays.copyOfRange(words, 0, half));
            String second = String.join(" ", java.util.Arrays.copyOfRange(words, half, words.length));
            if (alterWords && i == 2)
                second = second + " extra";
            script.add(new PageAudioScript(i + 1, List.of(
                new AudioFragment(first, 0.5, 0.4, 0.8), new AudioFragment(second, 1.5, 0.4, 0.8))));
        }
        return script;
    }

    /** Repositorio en memoria con la semantica del puerto. */
    private static class InMemoryNarrations implements NarrationRepository {
        private NarrationStatus status;
        private String voice;
        private Map<Integer, byte[]> audios = new HashMap<>();
        private int publishCalls;

        @Override
        public StoryNarration markInProgress(Long storyId) {
            status = NarrationStatus.IN_PROGRESS;
            voice = null;
            audios = new HashMap<>();
            return current(storyId);
        }

        @Override
        public Optional<StoryNarration> find(Long storyId) {
            return status == null ? Optional.empty() : Optional.of(current(storyId));
        }

        @Override
        public List<StoryNarration> findInProgress() {
            return List.of();
        }

        @Override
        public void publish(Long storyId, String voiceName, Map<Integer, byte[]> audiosByPage) {
            publishCalls++;
            status = NarrationStatus.COMPLETED;
            voice = voiceName;
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
            audios = new HashMap<>();
        }

        @Override
        public void delete(Long storyId) {
            status = null;
            voice = null;
            audios = new HashMap<>();
        }

        private StoryNarration current(Long storyId) {
            return new StoryNarration(storyId, status, voice, new ArrayList<>(audios.keySet()));
        }
    }
}
