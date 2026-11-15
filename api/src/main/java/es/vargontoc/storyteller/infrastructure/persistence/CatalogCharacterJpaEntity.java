package es.vargontoc.storyteller.infrastructure.persistence;

import java.util.List;
import es.vargontoc.storyteller.domain.model.ActorMetadata;
import es.vargontoc.storyteller.infrastructure.converters.ActorMetadataConverter;
import es.vargontoc.framework.persistence.jpa.entity.AutoIdJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

@Entity @Table(name = "catalog_character")
public class CatalogCharacterJpaEntity extends AutoIdJpaEntity {
    @Column(nullable = false, length = 100) private String name;
    @Column(name = "narrative_description", nullable = false, length = 1000) private String narrativeDescription;
    @Column(name = "visual_description", nullable = false, length = 1000) private String visualDescription;
    @Convert(converter = ActorMetadataConverter.class) @ColumnTransformer(write = "?::jsonb") @Column(name = "metadata", columnDefinition = "jsonb") private ActorMetadata metadata;
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getNarrativeDescription(){return narrativeDescription;} public void setNarrativeDescription(String v){narrativeDescription=v;}
    public String getVisualDescription(){return visualDescription;} public void setVisualDescription(String v){visualDescription=v;}
    public ActorMetadata getMetadata(){return metadata;} public void setMetadata(ActorMetadata v){metadata=v;}
}
