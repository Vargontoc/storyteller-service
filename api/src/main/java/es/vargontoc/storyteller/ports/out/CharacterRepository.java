package es.vargontoc.storyteller.ports.out;

import es.vargontoc.storyteller.domain.model.Actor;

public interface CharacterRepository {
    
    Actor update(Actor character);
}
