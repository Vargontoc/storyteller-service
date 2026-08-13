package es.vargontoc.storyteller.shared.mappers;

import es.vargontoc.storyteller.shared.BaseEntity;
import es.vargontoc.storyteller.shared.BaseModel;

public interface IMapper<T1 extends BaseEntity, T2 extends BaseModel> {
    
    T1 toEntity(T2 model);

    T2 toModel(T1 entity);
}
