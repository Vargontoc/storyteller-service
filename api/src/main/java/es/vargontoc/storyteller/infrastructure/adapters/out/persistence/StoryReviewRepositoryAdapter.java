package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryReviewRepository;
import es.vargontoc.storyteller.domain.enums.RevisionStatus;
import es.vargontoc.storyteller.domain.model.ActorMetadata;
import es.vargontoc.storyteller.domain.model.StoryOutline;
import es.vargontoc.storyteller.domain.model.StoryReview;
import es.vargontoc.storyteller.domain.response.CharacterAgentResult;
import es.vargontoc.storyteller.domain.response.StoryReviewAgentResult;
import es.vargontoc.storyteller.infrastructure.mappers.StoryReviewMapper;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryReviewJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryReviewJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryReviewJpaEntity.CharacterDraft;
import es.vargontoc.storyteller.shared.exceptions.ResourceNotFoundException;

@Repository
public class StoryReviewRepositoryAdapter implements StoryReviewRepository {

    private final StoryReviewJpaRepository repository;
    private final StoryJpaRepository storyRepository;
    private final StoryReviewMapper mapper;
    private final StoryOutlineRepository outlineRepository;

    public StoryReviewRepositoryAdapter(
        StoryReviewJpaRepository repository,
        StoryJpaRepository storyRepository,
        StoryReviewMapper mapper,
        StoryOutlineRepository outlineRepository) {
        this.outlineRepository = outlineRepository;
        this.repository = repository;
        this.mapper = mapper;
        this.storyRepository = storyRepository;
    }
    
    @Override
    @Transactional
    public void changeStatus(Long storyId, RevisionStatus status) {
        repository.getActiveStoryReviewByStoryId(storyId).ifPresent(x -> {
            // Confirmar publica la candidata como escaleta vigente; descartar la deja como estaba.
            if (status == RevisionStatus.CONFIRMED && !x.getCandidateBeats().isEmpty()) {
                outlineRepository.save(storyId, mapper.toOutline(x));
            }
            x.setStatus(status);
            x.setUpdatedAt(LocalDateTime.now());
            repository.save(x);
        });
    }

    @Override
    public StoryReview getPendingReview(Long storyId) {
        Optional<StoryReviewJpaEntity> anyReview = repository.getActiveStoryReviewByStoryId(storyId);
        if(anyReview.isPresent())
            return mapper.toModel(anyReview.get());
        return null;
    }

    @Override
    public StoryReview getReview(Long idReview) {
        StoryReviewJpaEntity entity = repository.findById(idReview).orElseThrow(() -> {
            throw new ResourceNotFoundException("Story review not found with id: " + idReview);
        });

        return mapper.toModel(entity);
    }

    @Override
    public StoryReview createStoryReview(StoryReviewAgentResult result, Long storyId, String hintRequest) {
        StoryJpaEntity story = storyRepository.findById(storyId).get();
        StoryReviewJpaEntity entity = StoryReviewJpaEntity.candidate(story, hintRequest, result.hintAccepted(), result.rejectionReason(), result.title(), result.synopsis(), getCharacterDrafts(result.characters()));

        return mapper.toModel(repository.save(entity));
    }

    @Override
    @Transactional
    public StoryReview createOutlineReview(Long storyId, String hint, boolean hintAccepted, String rejectedReason,
            String title, String synopsis, StoryOutline candidate) {
        StoryJpaEntity story = storyRepository.findById(storyId)
            .orElseThrow(() -> new ResourceNotFoundException("Story not found with id: " + storyId));
        StoryReviewJpaEntity entity = StoryReviewJpaEntity.candidate(story, hint, hintAccepted, rejectedReason, title, synopsis, new ArrayList<>());
        if (hintAccepted) {
            if (candidate == null || !candidate.isComplete()) {
                throw new IllegalArgumentException("Incomplete candidate outline for story " + storyId);
            }
            mapper.fillOutline(entity, candidate, changedBeats(outlineRepository.find(storyId).orElse(null), candidate));
        }
        return mapper.toModel(repository.save(entity));
    }

    /** Paginas (desde 1) cuyo beat difiere del vigente. Sin vigente, todas las de la candidata son nuevas. */
    private List<Integer> changedBeats(StoryOutline current, StoryOutline candidate) {
        List<Integer> changed = new ArrayList<>();
        int size = Math.max(candidate.beats().size(), current == null ? 0 : current.beats().size());
        for (int i = 0; i < size; i++) {
            var before = current != null && i < current.beats().size() ? current.beats().get(i) : null;
            var after = i < candidate.beats().size() ? candidate.beats().get(i) : null;
            if (before == null || !before.equals(after)) {
                changed.add(i + 1);
            }
        }
        return changed;
    }

    private List<CharacterDraft> getCharacterDrafts(List<CharacterAgentResult> characters){
        return characters.stream()
            .map(c -> new CharacterDraft(c.mainCharacter(), c.name(), c.narrativeDescription(), c.visualDescription(), new ActorMetadata(c.visualDescriptionEn(),c.visualAttributes())))
            .collect(Collectors.toList());
    }


    
}
