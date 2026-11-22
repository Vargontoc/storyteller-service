package es.vargontoc.storyteller.application.service;

import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import es.vargontoc.storyteller.application.ports.in.generator.StoryNarrationUseCase;
import es.vargontoc.storyteller.application.ports.in.generator.VoiceLibraryUseCase;
import es.vargontoc.storyteller.application.ports.out.GenerationGatePort;
import es.vargontoc.storyteller.application.ports.out.GenerationGatePort.GenerationType;
import es.vargontoc.storyteller.application.ports.out.NarrationExecutor;
import es.vargontoc.storyteller.application.ports.out.persistence.NarrationRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.domain.enums.NarrationDisabledReason;
import es.vargontoc.storyteller.domain.enums.NarrationRejectionReason;
import es.vargontoc.storyteller.domain.enums.NarrationStatus;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryNarration;
import es.vargontoc.storyteller.domain.enums.PageStatus;
import es.vargontoc.storyteller.domain.response.NarrationState;
import es.vargontoc.storyteller.domain.response.VoiceEntry;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import es.vargontoc.storyteller.shared.exceptions.NarrationRejectedException;
import jakarta.transaction.Transactional;

/**
 * Narracion de los cuentos con escaleta: estado (siempre calculado desde las paginas interiores actuales; la
 * portada no cuenta), consultas y borrado que usa StoryPageService, e inicio de la narracion/renarracion.
 * La ejecucion real la hace NarrationExecutor (TASK-021).
 */
@Service
@Transactional
public class StoryNarrationService implements StoryNarrationUseCase {

    private final StoryRepository stories;
    private final StoryOutlineRepository outlines;
    private final NarrationRepository narrations;
    private final GenerationGatePort gate;
    private final VoiceLibraryUseCase voices;
    private final NarrationNotifier notifier;
    private final NarrationExecutor executor;

    public StoryNarrationService(StoryRepository stories, StoryOutlineRepository outlines,
            NarrationRepository narrations, GenerationGatePort gate, VoiceLibraryUseCase voices,
            NarrationNotifier notifier, NarrationExecutor executor) {
        this.stories = stories;
        this.outlines = outlines;
        this.narrations = narrations;
        this.gate = gate;
        this.voices = voices;
        this.notifier = notifier;
        this.executor = executor;
    }

    @Override
    public NarrationState start(Long storyId, Long voiceId) {
        // 1. Validar antes de pedir el turno: nada se toca si algo falla.
        NarrationDisabledReason disabled = getState(storyId).disabledReason();
        if (disabled != null)
            throw reject(NarrationRejectionReason.valueOf(disabled.name()), "No se puede narrar: " + disabled);
        VoiceEntry voice = chooseVoice(voiceId);

        // 2. Turno global. Ocupado: rechazo habitual, sin tocar audio ni voz.
        if (!gate.tryAcquire(GenerationType.NARRATION, storyId))
            throw reject(NarrationRejectionReason.GENERATION_IN_PROGRESS, "Hay una generación en curso");

        // 3. Turno concedido: cualquier fallo antes de entregar el trabajo libera el turno.
        try {
            byte[] sample = voices.eligibleSample(voiceId)
                .orElseThrow(() -> reject(NarrationRejectionReason.VOICE_NOT_AVAILABLE, "Voz no disponible"));
            narrations.markInProgress(storyId);   // descarta el audio y la voz anteriores
            notifier.started(storyId);
            executor.execute(storyId, voice.name(), sample);
        } catch (RuntimeException e) {
            gate.release();
            throw e;
        }
        return getState(storyId);
    }

    private VoiceEntry chooseVoice(Long voiceId) {
        var eligible = voices.eligible();
        if (eligible.isEmpty())
            throw reject(NarrationRejectionReason.NO_VOICES_AVAILABLE, "No hay voces en español disponibles");
        return eligible.stream().filter(v -> Objects.equals(v.id(), voiceId)).findFirst()
            .orElseThrow(() -> reject(NarrationRejectionReason.VOICE_NOT_AVAILABLE, "Voz no disponible"));
    }

    private static NarrationRejectedException reject(NarrationRejectionReason reason, String message) {
        return new NarrationRejectedException(reason, message);
    }

    @Override
    public NarrationState getState(Long storyId) {
        Story story = stories.getStory(storyId);
        Optional<StoryNarration> narration = narrations.find(storyId);
        boolean narrating = narration.map(n -> n.getStatus() == NarrationStatus.IN_PROGRESS).orElse(false);
        boolean hasAudio = narration.map(StoryNarration::hasAudio).orElse(false);
        String voice = hasAudio ? narration.get().getVoiceName() : null;
        NarrationStatus last = narration.map(StoryNarration::getStatus).orElse(null);

        NarrationDisabledReason reason = disabledReason(story, narrating);
        return new NarrationState(storyId, story.hasOutline(), reason == null, reason, narrating, hasAudio, voice, last);
    }

    @Override
    public boolean isNarrating(Long storyId) {
        return narrations.find(storyId).map(n -> n.getStatus() == NarrationStatus.IN_PROGRESS).orElse(false);
    }

    @Override
    public boolean hasAudio(Long storyId) {
        return narrations.find(storyId).map(StoryNarration::hasAudio).orElse(false);
    }

    @Override
    public byte[] getPageAudio(Long storyId, int page) {
        // La portada (0) nunca tiene audio; en curso o con error el repositorio no guarda audio.
        if (page < 1)
            throw audioNotFound();
        return narrations.findAudio(storyId, page).orElseThrow(StoryNarrationService::audioNotFound);
    }

    private static AppException audioNotFound() {
        return new AppException("AUDIO_NOT_FOUND: no hay audio publicado para la página", HttpStatus.NOT_FOUND);
    }

    @Override
    public void deleteAudio(Long storyId) {
        narrations.delete(storyId);
    }

    private NarrationDisabledReason disabledReason(Story story, boolean narrating) {
        if (!story.hasOutline())
            return NarrationDisabledReason.NO_OUTLINE;
        if (narrating)
            return NarrationDisabledReason.IN_PROGRESS;
        return allInteriorAccepted(story) ? null : NarrationDisabledReason.PAGES_NOT_ACCEPTED;
    }

    /** Todas las paginas interiores de la escaleta (1..beats) existen y estan aceptadas; la portada no bloquea. */
    private boolean allInteriorAccepted(Story story) {
        int beats = outlines.find(story.getId()).map(o -> o.beats().size()).orElse(0);
        if (beats == 0)
            return false;
        long accepted = story.getPages().stream()
            .filter(p -> p.getPage() >= 1 && p.getPage() <= beats && p.getPageStatus() == PageStatus.ACCEPTED)
            .count();
        return accepted == beats;
    }
}
