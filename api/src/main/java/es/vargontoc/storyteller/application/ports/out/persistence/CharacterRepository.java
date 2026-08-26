package es.vargontoc.storyteller.application.ports.out.persistence;

import java.util.List;

import es.vargontoc.storyteller.domain.model.Actor;
import es.vargontoc.storyteller.domain.response.CharacterAgentResult;

public interface CharacterRepository {
    
    Actor createActor(Long idStory, CharacterAgentResult result);

    Actor getActor(Long id);

    List<Actor> getActorsByStory(Long storyId);

    Actor update(Actor character);

    void setImagePath(Long id, String path);
}
