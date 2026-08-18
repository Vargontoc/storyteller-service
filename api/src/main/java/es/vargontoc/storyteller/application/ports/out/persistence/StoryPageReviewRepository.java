package es.vargontoc.storyteller.application.ports.out.persistence;

import es.vargontoc.storyteller.domain.enums.PageReviewTarget;
import es.vargontoc.storyteller.domain.enums.RevisionStatus;
import es.vargontoc.storyteller.domain.model.StoryPageReview;
import es.vargontoc.storyteller.domain.response.StoryPageReviewAgentResult;

public interface StoryPageReviewRepository {
    
    StoryPageReview getPendingReview(Long pageId);

    void changeStatus(Long pageId, RevisionStatus status);

    StoryPageReview createReview(StoryPageReviewAgentResult result, Long pageId, PageReviewTarget taeget, String hint);
}
