package es.vargontoc.storyteller.application.ports.out.persistence;

import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.response.StoryPageAgentResult;

public interface StoryPageRepository {
    
    StoryPage create(StoryPageAgentResult result, Long storyId, int page);

    StoryPage update(StoryPage page);
}
