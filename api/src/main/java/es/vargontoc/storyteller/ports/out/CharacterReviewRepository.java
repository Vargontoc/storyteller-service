package es.vargontoc.storyteller.ports.out;

import es.vargontoc.storyteller.domain.model.ActorReview;
import es.vargontoc.storyteller.domain.model.RevisionStatus;
import es.vargontoc.storyteller.domain.response.CharacterReviewAgentResult;
import es.vargontoc.storyteller.infrastructure.dto.ReviewCharacterRequestDto;

public interface CharacterReviewRepository {
    
    void changeStatus(Long characterId, RevisionStatus status);

    ActorReview getPendingReview(Long characterId);

    ActorReview getReview(Long idReview);

    ActorReview createReview(CharacterReviewAgentResult result, Long characterId, ReviewCharacterRequestDto request);
}
