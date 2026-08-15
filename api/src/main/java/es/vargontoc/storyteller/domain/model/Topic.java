package es.vargontoc.storyteller.domain.model;

import es.vargontoc.storyteller.shared.BaseModel;

public class Topic  extends BaseModel  {
    
    private Long id;
    private String type;
    private String description;
    
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return String.format("type: %s, description: %s", getType(), getDescription());
    }
}
