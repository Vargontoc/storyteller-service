package es.vargontoc.storyteller.shared.mappers;

import java.util.List;

import es.vargontoc.storyteller.shared.BaseEntity;
import es.vargontoc.storyteller.shared.BaseModel;

public abstract class AbstractMapper<T1 extends BaseEntity, T2 extends BaseModel> implements IMapper<T1, T2> {
    
    public List<T1> toEntity(List<T2> models) {
        return models.stream().map(this::toEntity).toList();
    }

    public List<T2> toModel(List<T1> entities){
        return entities.stream().map(this::toModel).toList();
    }
}
