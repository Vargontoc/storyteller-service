package es.vargontoc.storyteller.infrastructure.mappers;

import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaEntity;
import es.vargontoc.storyteller.shared.mappers.AbstractMapper;

@Component
public class StoryMapper extends AbstractMapper<StoryJpaEntity, Story> {

    private final CharacterMapper characterMapper;
    private final StoryPageMapper pageMapper;

    public StoryMapper(
        CharacterMapper characterMapper,
        StoryPageMapper pageMapper) {
        this.characterMapper = characterMapper;
        this.pageMapper = pageMapper;
    }

    @Override
    public StoryJpaEntity toEntity(Story model) {
        StoryJpaEntity target = new StoryJpaEntity();
        target.setId(model.getId());
        target.setTitle(model.getTitle());
        target.setSynopsis(model.getSummary());
        target.setSize(model.getSize());
        target.setCharacters(characterMapper.toEntity(model.getCharacters()));
        target.setPages(pageMapper.toEntity(model.getPages()));
        return target;
    }

    @Override
    public Story toModel(StoryJpaEntity entity) {
        Story target = new Story();
        target.setId(entity.getId());
        target.setSize(entity.getSize());
        target.setSummary(entity.getSynopsis());
        target.setTitle(entity.getTitle());

        target.setCharacters(characterMapper.toModel(entity.getCharacters()));
        target.setPages(pageMapper.toModel(entity.getPages()));
        return target;
    }
    
}
