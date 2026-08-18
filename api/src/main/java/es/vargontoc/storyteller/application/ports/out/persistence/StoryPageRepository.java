package es.vargontoc.storyteller.application.ports.out.persistence;

import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.model.StoryPageReview;
import es.vargontoc.storyteller.domain.response.StoryPageAgentResult;

public interface StoryPageRepository {
    
    StoryPage create(StoryPageAgentResult result, Long storyId, int page);

    StoryPage getPage(Long idPage);

    StoryPage update(StoryPage page);

    StoryPage updateWithReview(Long idPage, StoryPageReview review);

    void setImagePath(Long idPage, String path);

    void setAudioPath(Long id, String path);
    
}
