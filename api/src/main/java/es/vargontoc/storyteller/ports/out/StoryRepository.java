package es.vargontoc.storyteller.ports.out;

import es.vargontoc.storyteller.domain.Story;
import es.vargontoc.storyteller.domain.StoryAgentResult;
import es.vargontoc.storyteller.domain.StorySize;

public interface StoryRepository {
    
    Story create(StoryAgentResult result, StorySize size, Long topic);

    Story update(Story story);

    Story getStory(Long idStory);

}
