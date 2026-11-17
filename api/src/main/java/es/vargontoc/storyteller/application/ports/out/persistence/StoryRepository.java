package es.vargontoc.storyteller.application.ports.out.persistence;

import es.vargontoc.storyteller.domain.enums.StorySize;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.response.StoryAgentResult;

public interface StoryRepository {
    
    Story create(StoryAgentResult result, StorySize size, Long topic);
    default Story create(StoryAgentResult result, StorySize size, Long topic, Long catalogCharacterId) {
        return create(result, size, topic);
    }

    Story update(Story story);

    Story getStory(Long idStory);

    void delete(Long idStory);

}
