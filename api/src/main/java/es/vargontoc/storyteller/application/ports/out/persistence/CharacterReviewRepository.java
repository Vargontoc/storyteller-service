package es.vargontoc.storyteller.application.ports.out.persistence;

import es.vargontoc.storyteller.domain.enums.CharacterReviewTarget;
import es.vargontoc.storyteller.domain.enums.RevisionStatus;
import es.vargontoc.storyteller.domain.model.ActorReview;
import es.vargontoc.storyteller.domain.response.CharacterReviewAgentResult;

public interface CharacterReviewRepository {
    
    void changeStatus(Long characterId, RevisionStatus status);

    ActorReview getPendingReview(Long characterId);

    ActorReview getReview(Long idReview);

    ActorReview createReview(CharacterReviewAgentResult result, Long characterId, CharacterReviewTarget target, String hint);
}
