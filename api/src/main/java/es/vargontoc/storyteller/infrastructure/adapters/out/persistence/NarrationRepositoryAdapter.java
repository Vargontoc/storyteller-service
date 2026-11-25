package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.application.ports.out.persistence.NarrationRepository;
import es.vargontoc.storyteller.domain.enums.NarrationStatus;
import es.vargontoc.storyteller.domain.model.StoryNarration;
import es.vargontoc.storyteller.infrastructure.mappers.StoryNarrationMapper;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryNarrationAudioJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryNarrationJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryNarrationJpaRepository;
import es.vargontoc.storyteller.shared.exceptions.ResourceNotFoundException;

@Repository
public class NarrationRepositoryAdapter implements NarrationRepository {

    private final StoryNarrationJpaRepository narrations;
    private final StoryJpaRepository stories;
    private final ResourceStorage storage;
    private final StoryNarrationMapper mapper;

    public NarrationRepositoryAdapter(StoryNarrationJpaRepository narrations, StoryJpaRepository stories,
            ResourceStorage storage, StoryNarrationMapper mapper) {
        this.narrations = narrations;
        this.stories = stories;
        this.storage = storage;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public StoryNarration markInProgress(Long storyId) {
        return mapper.toModel(reset(storyId, NarrationStatus.IN_PROGRESS));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoryNarration> find(Long storyId) {
        return narrations.findByStoryId(storyId).map(mapper::toModel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoryNarration> findInProgress() {
        return narrations.findByStatus(NarrationStatus.IN_PROGRESS).stream().map(mapper::toModel).toList();
    }

    @Override
    @Transactional
    public void publish(Long storyId, String voiceName, Map<Integer, byte[]> audiosByPage) {
        if (audiosByPage == null || audiosByPage.isEmpty() || audiosByPage.keySet().stream().anyMatch(p -> p < 1))
            throw new IllegalArgumentException("Solo se publica audio de paginas interiores (page >= 1)");

        StoryNarrationJpaEntity narration = narrations.findByStoryId(storyId)
                .filter(n -> n.getStatus() == NarrationStatus.IN_PROGRESS)
                .orElseThrow(() -> new IllegalStateException("No hay narracion en curso para el cuento " + storyId));

        try {
            audiosByPage.forEach((page, bytes) -> {
                StoryNarrationAudioJpaEntity audio = new StoryNarrationAudioJpaEntity();
                audio.setNarration(narration);
                audio.setPage(page);
                audio.setAudioAsset(storage.saveNarrationAudio(storyId, page, bytes));
                narration.getAudios().add(audio);
            });
            narration.setVoiceName(voiceName);
            narration.setStatus(NarrationStatus.COMPLETED);
            narrations.saveAndFlush(narration);
        } catch (RuntimeException e) {
            // La transaccion se deshace; los ficheros recien escritos no, asi que se retiran.
            storage.deleteNarrationAudio(storyId);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<byte[]> findAudio(Long storyId, int page) {
        return narrations.findByStoryId(storyId)
                .flatMap(n -> n.getAudios().stream().filter(a -> a.getPage() == page).findFirst())
                .map(a -> storage.getResource(a.getAudioAsset()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findAudioPath(Long storyId, int page) {
        return narrations.findByStoryId(storyId)
                .flatMap(n -> n.getAudios().stream().filter(a -> a.getPage() == page).findFirst())
                .map(StoryNarrationAudioJpaEntity::getAudioAsset);
    }

    @Override
    @Transactional
    public void markError(Long storyId) {
        reset(storyId, NarrationStatus.ERROR);
    }

    @Override
    @Transactional
    public void delete(Long storyId) {
        narrations.findByStoryId(storyId).ifPresent(narrations::delete);
        deleteFilesAfterCommit(storyId);
    }

    /** Deja la narracion (creandola si no existe) en el estado dado, sin audio ni voz. */
    private StoryNarrationJpaEntity reset(Long storyId, NarrationStatus status) {
        StoryNarrationJpaEntity narration = narrations.findByStoryId(storyId).orElseGet(() -> {
            StoryNarrationJpaEntity created = new StoryNarrationJpaEntity();
            created.setStory(stories.findById(storyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Story not found with id: " + storyId)));
            return created;
        });
        narration.getAudios().clear();
        narration.setVoiceName(null);
        narration.setStatus(status);
        StoryNarrationJpaEntity saved = narrations.saveAndFlush(narration);
        deleteFilesAfterCommit(storyId);
        return saved;
    }

    /** Los ficheros no hacen rollback: se borran solo si la transaccion de la operacion se confirma. */
    private void deleteFilesAfterCommit(Long storyId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            storage.deleteNarrationAudio(storyId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                storage.deleteNarrationAudio(storyId);
            }
        });
    }
}
