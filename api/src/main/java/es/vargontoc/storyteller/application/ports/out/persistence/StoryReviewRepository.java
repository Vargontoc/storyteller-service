package es.vargontoc.storyteller.application.ports.out.persistence;


import es.vargontoc.storyteller.domain.model.RevisionStatus;
import es.vargontoc.storyteller.domain.model.StoryReview;
import es.vargontoc.storyteller.domain.response.StoryReviewAgentResult;

public interface StoryReviewRepository {
    
    void changeStatus(Long storyId, RevisionStatus status);

    StoryReview getPendingReview(Long storyId);

    StoryReview createStoryReview(StoryReviewAgentResult request, Long storyId, String hintRequest);

    StoryReview getReview(Long idReview);
}
