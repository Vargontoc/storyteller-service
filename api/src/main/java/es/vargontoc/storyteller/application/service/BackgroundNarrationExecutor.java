package es.vargontoc.storyteller.application.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import es.vargontoc.storyteller.application.ports.out.GenerationGatePort;
import es.vargontoc.storyteller.application.ports.out.NarrationExecutor;
import es.vargontoc.storyteller.application.ports.out.external.NarratorAgent;
import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.application.ports.out.external.SpeechSynthesizer;
import es.vargontoc.storyteller.application.ports.out.persistence.NarrationRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.application.utils.AudioAssembler;
import es.vargontoc.storyteller.domain.enums.AudioFragmentKind;
import es.vargontoc.storyteller.domain.model.AudioFragment;
import es.vargontoc.storyteller.domain.model.PageAudioScript;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.request.SpeechSynthesisRequest;
import es.vargontoc.storyteller.domain.service.AudioScriptValidator;
import es.vargontoc.storyteller.shared.exceptions.AppException;

/**
 * Ejecuta la narracion en segundo plano (TASK-021): narrador, validacion del guion, sintesis por fragmento y
 * publicacion atomica al final. Los audios y el guion viven solo en memoria hasta publicar. Ante cualquier
 * fallo deja el cuento sin audio ni voz, notifica el error y libera siempre el turno de GenerationGate.
 * No hay reintento.
 */
@Service
public class BackgroundNarrationExecutor implements NarrationExecutor {

    private static final Logger log = LoggerFactory.getLogger(BackgroundNarrationExecutor.class);
    private static final String LANGUAGE = "es";
    private static final Duration FRAGMENT_PAUSE = Duration.ofMillis(160);

    private final TaskExecutor executor;
    private final StoryRepository stories;
    private final StoryOutlineRepository outlines;
    private final NarratorAgent narrator;
    private final SpeechSynthesizer synthesizer;
    private final OllamaPort ollama;
    private final NarrationRepository narrations;
    private final NarrationNotifier notifier;
    private final GenerationGatePort gate;
    private final AudioScriptValidator validator = new AudioScriptValidator();

    public BackgroundNarrationExecutor(TaskExecutor executor, StoryRepository stories,
            StoryOutlineRepository outlines, NarratorAgent narrator, SpeechSynthesizer synthesizer,
            OllamaPort ollama, NarrationRepository narrations, NarrationNotifier notifier,
            GenerationGatePort gate) {
        this.executor = executor;
        this.stories = stories;
        this.outlines = outlines;
        this.narrator = narrator;
        this.synthesizer = synthesizer;
        this.ollama = ollama;
        this.narrations = narrations;
        this.notifier = notifier;
        this.gate = gate;
    }

    @Override
    public void execute(Long storyId, String voiceName, byte[] voiceSample) {
        Runnable job = () -> narrate(storyId, voiceName, voiceSample);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            submit(storyId, job);
            return;
        }
        // Se lanza tras el commit del estado EN CURSO; si la transaccion se deshace, el turno se libera.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                submit(storyId, job);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED)
                    gate.release();
            }
        });
    }

    private void submit(Long storyId, Runnable job) {
        try {
            executor.execute(job);
        } catch (RuntimeException e) {
            log.error("No se pudo lanzar la narracion del cuento {}", storyId, e);
            fail(storyId);
            gate.release();
        }
    }

    /** Cuerpo de la narracion: sincrono, el ejecutor configurado lo invoca en segundo plano. */
    void narrate(Long storyId, String voiceName, byte[] voiceSample) {
        try {
            List<String> texts = acceptedInteriorTexts(storyId);

            List<PageAudioScript> script = narrator.narrate(texts);
            List<List<String>> fragments = script.stream()
                .map(p -> p.fragments().stream().map(AudioFragment::text).toList())
                .toList();
            AudioScriptValidator.Result result = validator.validate(texts, fragments);
            if (!result.valid())
                throw new AppException("El guion de audio altera el texto de la página " + result.failedPage(),
                    HttpStatus.BAD_GATEWAY);

            ollama.stopAllServices();   // libera la VRAM del narrador antes de sintetizar (una vez)

            Map<Integer, byte[]> audios = new HashMap<>();
            for (PageAudioScript page : script)
                audios.put(page.page(), synthesizePage(page, voiceSample));

            narrations.publish(storyId, voiceName, audios);
            notify(() -> notifier.completed(storyId, voiceName));
        } catch (RuntimeException e) {
            log.error("La narracion del cuento {} ha fallado", storyId, e);
            fail(storyId);
        } finally {
            gate.release();
        }
    }

    private byte[] synthesizePage(PageAudioScript page, byte[] voiceSample) {
        List<AudioFragment> fragments = page.fragments();
        List<byte[]> clips = new ArrayList<>(Math.max(1, fragments.size() * 2 - 1));
        for (int i = 0; i < fragments.size(); i++) {
            AudioFragment fragment = withProfile(fragments.get(i));
            clips.add(synthesizer.synthesize(new SpeechSynthesisRequest(
                fragment.text(), LANGUAGE, voiceSample, fragment.exaggeration(), fragment.cfgWeight(),
                fragment.temperature())));
            if (i < fragments.size() - 1)
                clips.add(AudioAssembler.silence(FRAGMENT_PAUSE));
        }
        return AudioAssembler.concat(clips);
    }

    /** Perfil fijo por tipo: la narración es más calmada y el diálogo, más expresivo. */
    private AudioFragment withProfile(AudioFragment fragment) {
        if (fragment.kind() == AudioFragmentKind.DIALOGUE)
            return new AudioFragment(fragment.text(), clamp(fragment.exaggeration(), 0.7, 1.2),
                clamp(fragment.cfgWeight(), 0.3, 0.45), clamp(fragment.temperature(), 0.8, 0.9),
                fragment.kind());
        return new AudioFragment(fragment.text(), clamp(fragment.exaggeration(), 0.35, 0.6),
            clamp(fragment.cfgWeight(), 0.45, 0.6), clamp(fragment.temperature(), 0.6, 0.8),
            fragment.kind());
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Texto aceptado de las paginas interiores 1..beats, en orden; la portada no se incluye. */
    private List<String> acceptedInteriorTexts(Long storyId) {
        int beats = outlines.find(storyId).map(o -> o.beats().size()).orElse(0);
        List<String> texts = stories.getStory(storyId).getPages().stream()
            .filter(p -> p.getPage() >= 1 && p.getPage() <= beats)
            .sorted(Comparator.comparingInt(StoryPage::getPage))
            .map(StoryPage::getText)
            .toList();
        if (beats == 0 || texts.size() != beats)
            throw new AppException("No hay texto aceptado de todas las páginas interiores", HttpStatus.CONFLICT);
        return texts;
    }

    /** Deja el cuento sin audio ni voz y avisa; ningun fallo secundario impide liberar el turno. */
    private void fail(Long storyId) {
        try {
            narrations.markError(storyId);
        } catch (RuntimeException e) {
            log.error("No se pudo marcar el error de la narracion del cuento {}", storyId, e);
        }
        notify(() -> notifier.failed(storyId));
    }

    private void notify(Runnable notification) {
        try {
            notification.run();
        } catch (RuntimeException e) {
            log.warn("No se pudo difundir el evento de narracion", e);
        }
    }
}
