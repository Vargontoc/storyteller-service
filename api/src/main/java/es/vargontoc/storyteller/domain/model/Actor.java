package es.vargontoc.storyteller.domain.model;

import es.vargontoc.storyteller.shared.BaseModel;

public class Actor extends BaseModel {
    
    private Long id;
    private String name;
    private String narrativeDescription;
    private String visualDescription;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getNarrativeDescription() {
        return narrativeDescription;
    }
    public void setNarrativeDescription(String narrativeDescription) {
        this.narrativeDescription = narrativeDescription;
    }
    public String getVisualDescription() {
        return visualDescription;
    }
    public void setVisualDescription(String visualDescription) {
        this.visualDescription = visualDescription;
    }
}
