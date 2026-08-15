package es.vargontoc.storyteller.ports.out;

import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StorySize;
import es.vargontoc.storyteller.domain.response.StoryAgentResult;

public interface StoryRepository {
    
    Story create(StoryAgentResult result, StorySize size, Long topic);

    Story update(Story story);

    Story getStory(Long idStory);

}
