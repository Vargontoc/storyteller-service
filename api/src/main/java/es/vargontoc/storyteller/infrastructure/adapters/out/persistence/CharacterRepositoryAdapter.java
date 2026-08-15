package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import es.vargontoc.storyteller.application.ports.out.persistence.CharacterRepository;
import es.vargontoc.storyteller.domain.model.Actor;
import es.vargontoc.storyteller.infrastructure.mappers.CharacterMapper;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterJpaRepository;

@Repository
public class CharacterRepositoryAdapter implements CharacterRepository {

    private final CharacterJpaRepository repository;
    private final CharacterMapper mapper;

    public CharacterRepositoryAdapter(CharacterJpaRepository repository, CharacterMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }



    @Override
    public Actor update(Actor character) {
        
        CharacterJpaEntity stored = repository.findById(character.getId()).get();
        stored.setNarrativeDescription(character.getNarrativeDescription());
        stored.setVisualDescription(character.getVisualDescription());
        stored.setUpdatedAt(LocalDateTime.now());
        repository.save(stored);
        return character;
    }



    @Override
    public Actor getActor(Long id) {
        return mapper.toModel(repository.findById(id).get());
    }



    @Override
    public List<Actor> getActorsByStory(Long storyId) {
        return null;
    }
    
}
