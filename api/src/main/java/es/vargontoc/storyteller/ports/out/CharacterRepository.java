package es.vargontoc.storyteller.ports.out;

import es.vargontoc.storyteller.domain.CharacterModel;

public interface CharacterRepository {
    
    CharacterModel update(CharacterModel character);
}
