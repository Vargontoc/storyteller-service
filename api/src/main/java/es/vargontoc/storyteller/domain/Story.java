package es.vargontoc.storyteller.domain;

import java.util.List;

import es.vargontoc.storyteller.shared.BaseModel;

public class Story  extends BaseModel {
    
    private Long id;
    private String title;
    private List<CharacterModel> characters;
    private String summary;
    private StorySize size;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public List<CharacterModel> getCharacters() {
        return characters;
    }
    public void setCharacters(List<CharacterModel> characters) {
        this.characters = characters;
    }
    public String getSummary() {
        return summary;
    }
    public void setSummary(String summary) {
        this.summary = summary;
    }
    public StorySize getSize() {
        return size;
    }
    public void setSize(StorySize size) {
        this.size = size;
    }
}
