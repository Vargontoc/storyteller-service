package es.vargontoc.storyteller.domain.model;

import java.util.List;

import es.vargontoc.storyteller.shared.BaseModel;

public class Story  extends BaseModel {
    
    private Long id;
    private String title;
    private List<Actor> characters;
    private List<StoryPage> pages;
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
    public List<Actor> getCharacters() {
        return characters;
    }
    public void setCharacters(List<Actor> characters) {
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
    public List<StoryPage> getPages() {
        return pages;
    }
    public void setPages(List<StoryPage> pages) {
        this.pages = pages;
    }
}
