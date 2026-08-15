package es.vargontoc.storyteller.ports.out;

import es.vargontoc.storyteller.domain.CharacterReview;
import es.vargontoc.storyteller.domain.CharacterReviewAgentResult;
import es.vargontoc.storyteller.domain.RevisionStatus;
import es.vargontoc.storyteller.infrastructure.dto.ReviewCharacterRequestDto;

public interface CharacterReviewRepository {
    
    void changeStatus(Long characterId, RevisionStatus status);

    CharacterReview getPendingReview(Long characterId);

    CharacterReview getReview(Long idReview);

    CharacterReview createReview(CharacterReviewAgentResult result, Long characterId, ReviewCharacterRequestDto request);
}
