package es.vargontoc.storyteller.infrastructure.adapters;

import java.time.LocalDateTime;

import org.springframework.stereotype.Repository;

import es.vargontoc.storyteller.domain.CharacterModel;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterJpaRepository;
import es.vargontoc.storyteller.ports.out.CharacterRepository;

@Repository
public class CharacterRepositoryAdapter implements CharacterRepository {

    private final CharacterJpaRepository repository;


    public CharacterRepositoryAdapter(CharacterJpaRepository repository) {
        this.repository = repository;
    }



    @Override
    public CharacterModel update(CharacterModel character) {
        
        CharacterJpaEntity stored = repository.findById(character.getId()).get();
        stored.setNarrativeDescription(character.getNarrativeDescription());
        stored.setVisualDescription(character.getVisualDescription());
        stored.setUpdatedAt(LocalDateTime.now());
        repository.save(stored);
        return character;
    }
    
}
