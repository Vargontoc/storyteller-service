package es.vargontoc.storyteller.infrastructure.persistence;

import es.vargontoc.storyteller.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "topic")
public class TopicJpaEntity extends BaseEntity {
    
    @Column(name = "name", nullable=false)
    private String name;
    @Column(name= "description", nullable=false)
    private String description;
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    
}
