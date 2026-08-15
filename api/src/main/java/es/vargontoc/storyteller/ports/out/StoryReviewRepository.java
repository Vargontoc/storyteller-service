package es.vargontoc.storyteller.ports.out;


import es.vargontoc.storyteller.domain.RevisionStatus;
import es.vargontoc.storyteller.domain.StoryReview;
import es.vargontoc.storyteller.domain.StoryReviewAgentResult;

public interface StoryReviewRepository {
    
    void changeStatus(Long storyId, RevisionStatus status);

    StoryReview getPendingReview(Long storyId);

    StoryReview createStoryReview(StoryReviewAgentResult request, Long storyId, String hintRequest);

    StoryReview getReview(Long idReview);
}
