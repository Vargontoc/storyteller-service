package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import es.vargontoc.storyteller.application.ports.out.persistence.CharacterReviewRepository;
import es.vargontoc.storyteller.domain.model.ActorReview;
import es.vargontoc.storyteller.domain.model.CharacterReviewTarget;
import es.vargontoc.storyteller.domain.model.RevisionStatus;
import es.vargontoc.storyteller.domain.response.CharacterReviewAgentResult;
import es.vargontoc.storyteller.infrastructure.mappers.CharacterReviewMapper;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterReviewJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterReviewJpaRepository;
import es.vargontoc.storyteller.shared.exceptions.ResourceNotFoundException;

@Repository
public class CharacterReviewRepositoryAdapter implements CharacterReviewRepository {

    private final CharacterReviewJpaRepository repository;
    private final CharacterJpaRepository characterRepository;
    private final CharacterReviewMapper mapper;

    public CharacterReviewRepositoryAdapter(
        CharacterReviewJpaRepository repository,
        CharacterReviewMapper mapper,
        CharacterJpaRepository characterRepository) {
        this.repository = repository;
        this.mapper = mapper;
        this.characterRepository = characterRepository;
    }


    @Override
    public void changeStatus(Long characterId, RevisionStatus status) {
        repository.getactiveReviewByCharacterId(characterId).ifPresent(x -> {
            x.setStatus(status);
            x.setUpdatedAt(LocalDateTime.now());
            repository.save(x);
        });
    }

    @Override
    public ActorReview getPendingReview(Long characterId) {
        Optional<CharacterReviewJpaEntity> entity = repository.getactiveReviewByCharacterId(characterId);
        if(entity.isPresent()){
            return mapper.toModel(entity.get());
        }
        return null;
    }

    @Override
    public ActorReview getReview(Long idReview) {
        CharacterReviewJpaEntity entity = repository.findById(idReview).orElseThrow(() -> {
            throw new ResourceNotFoundException("Character review not found with id: " + idReview);
        });

        return mapper.toModel(entity);
    }


    @Override
    public ActorReview createReview(CharacterReviewAgentResult result, Long characterId, CharacterReviewTarget target, String hint) {
        CharacterJpaEntity character = characterRepository.findById(characterId).get();
        CharacterReviewJpaEntity entity = CharacterReviewJpaEntity.candidate(character, hint, target, result.hintAccepted(), result.rejectionReason(), null, null);
        switch (target) {
            case NARRATIVE:
                entity.setCandidateNarrative(result.narrativeDescription());
                entity.setCandidateVisual(character.getVisualDescription());
                break;
            case VISUAL:
                entity.setCandidateNarrative(character.getNarrativeDescription());
                entity.setCandidateVisual(result.visualDescription());
                break;
            case BOTH:
                entity.setCandidateNarrative(result.narrativeDescription());
                entity.setCandidateVisual(result.visualDescription());
                break;
        }
        return mapper.toModel(repository.save(entity));
    }


    
}
