package es.vargontoc.storyteller.infrastructure.mappers;

import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.domain.model.ActorReview;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterReviewJpaEntity;
import es.vargontoc.storyteller.shared.mappers.AbstractReviewMapper;

@Component
public class CharacterReviewMapper extends AbstractReviewMapper<CharacterReviewJpaEntity, ActorReview> {

    @Override
    public CharacterReviewJpaEntity toEntity(ActorReview model) {
        CharacterReviewJpaEntity target = new CharacterReviewJpaEntity();
        target.setId(model.getId());
        target.setHint(model.getHint());
        target.setHintAccepted(model.isHintAccepted());
        target.setRejectedReason(model.getRejectedReason());
        target.setStatus(model.getStatus());
        target.setTarget(model.getTarget());
        target.setCandidateNarrative(model.getNarrativeDescription());
        target.setCandidateVisual(model.getVisualDescription());
        target.setMetadata(model.getMetadata());

        CharacterJpaEntity character = new CharacterJpaEntity();
        character.setId(model.getCharacterId());
        target.setCharacter(character);

        return target;
    }

    @Override
    public ActorReview toModel(CharacterReviewJpaEntity entity) {
        ActorReview target = new ActorReview();
        target.setId(entity.getId());
        target.setCharacterId(entity.getCharacter() != null ? entity.getCharacter().getId() : null);
        target.setHint(entity.getHint());
        target.setHintAccepted(entity.isHintAccepted());
        target.setRejectedReason(entity.getRejectedReason());
        target.setStatus(entity.getStatus());
        target.setTarget(entity.getTarget());
        target.setName(entity.getCharacter() != null ? entity.getCharacter().getName() : null);
        target.setNarrativeDescription(entity.getCandidateNarrative());
        target.setVisualDescription(entity.getCandidateVisual());
        target.setMetadata(entity.getMetadata());
        return target;
    }
}
