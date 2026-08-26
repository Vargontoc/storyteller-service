package es.vargontoc.storyteller.domain.model;

import es.vargontoc.storyteller.shared.BaseModel;

public class Actor extends BaseModel {
    
    private Long id;
    private boolean main;
    private Long storyId;
    private String name;
    private String narrativeDescription;
    private String visualDescription;
    private String image;
    private ActorMetadata metadata;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNarrativeDescription() { return narrativeDescription; }
    public void setNarrativeDescription(String narrativeDescription) { this.narrativeDescription = narrativeDescription; }
    public String getVisualDescription() { return visualDescription; }
    public void setVisualDescription(String visualDescription) { this.visualDescription = visualDescription; }
    public Long getStoryId() { return storyId; }
    public void setStoryId(Long storyId) { this.storyId = storyId; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public boolean isMain() { return main; }
    public void setMain(boolean main) { this.main = main; }
    public ActorMetadata getMetadata() { return metadata; }
    public void setMetadata(ActorMetadata metadata) { this.metadata = metadata; }
    
}
