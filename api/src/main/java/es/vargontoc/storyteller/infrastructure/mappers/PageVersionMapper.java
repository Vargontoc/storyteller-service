package es.vargontoc.storyteller.infrastructure.mappers;

import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.domain.model.PageVersion;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageVersionJpaEntity;
import es.vargontoc.storyteller.shared.mappers.AbstractMapper;

@Component
public class PageVersionMapper extends AbstractMapper<StoryPageVersionJpaEntity, PageVersion> {

    /** La pagina la asigna el adaptador. */
    @Override
    public StoryPageVersionJpaEntity toEntity(PageVersion model) {
        StoryPageVersionJpaEntity target = new StoryPageVersionJpaEntity();
        target.setId(model.getId());
        target.setText(model.getText());
        target.setComposition(model.getScene());
        target.setLengthWarning(model.getWarning());
        target.setCurrent(model.isCurrent());
        return target;
    }

    @Override
    public PageVersion toModel(StoryPageVersionJpaEntity entity) {
        PageVersion target = new PageVersion(entity.getText(), entity.getComposition(), entity.getLengthWarning(), entity.isCurrent());
        target.setId(entity.getId());
        return target;
    }
}
