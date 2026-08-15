package es.vargontoc.storyteller.application.ports.in.persistence;

import java.util.List;

import es.vargontoc.storyteller.domain.model.Actor;

public interface ActorUseCase {
    
    Actor getActor(Long id);

    List<Actor> getActors(Long storyId);

}
