package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import es.vargontoc.storyteller.application.ports.out.persistence.StoryReviewRepository;
import es.vargontoc.storyteller.domain.enums.RevisionStatus;
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

    public StoryReviewRepositoryAdapter(
        StoryReviewJpaRepository repository,
        StoryJpaRepository storyRepository,
        StoryReviewMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.storyRepository = storyRepository;
    }
    
    @Override
    public void changeStatus(Long storyId, RevisionStatus status) {
        repository.getActiveStoryReviewByStoryId(storyId).ifPresent(x -> {
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

    private List<CharacterDraft> getCharacterDrafts(List<CharacterAgentResult> characters){
        return characters.stream()
            .map(c -> new CharacterDraft(c.name(), c.narrativeDescription(), c.visualDescription()))
            .collect(Collectors.toList());
    }


    
}
