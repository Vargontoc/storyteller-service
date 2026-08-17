package es.vargontoc.storyteller.application.ports.out.persistence;

import java.util.List;

import es.vargontoc.storyteller.domain.model.Actor;

public interface CharacterRepository {
    
    Actor getActor(Long id);

    List<Actor> getActorsByStory(Long storyId);

    Actor update(Actor character);

    void setImagePath(Long id, String path);
}
