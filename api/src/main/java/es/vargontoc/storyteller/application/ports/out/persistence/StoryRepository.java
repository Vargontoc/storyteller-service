package es.vargontoc.storyteller.application.ports.out.persistence;

import es.vargontoc.storyteller.domain.enums.StorySize;
import es.vargontoc.storyteller.domain.enums.StoryStatus;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.response.StoryAgentResult;

public interface StoryRepository {
    
    Story create(StoryAgentResult result, StorySize size, Long topic);
    default Story create(StoryAgentResult result, StorySize size, Long topic, Long catalogCharacterId) {
        return create(result, size, topic);
    }

    Story update(Story story);

    /** Cambia solo el estado del guion del cuento. */
    void updateStatus(Long storyId, StoryStatus status);

    /** Aplica titulo y sinopsis de una escaleta confirmada y devuelve el guion a SCRIPT_PENDING. No toca personajes ni paginas. */
    void updateScript(Long storyId, String title, String synopsis);

    Story getStory(Long idStory);

    void delete(Long idStory);

}
