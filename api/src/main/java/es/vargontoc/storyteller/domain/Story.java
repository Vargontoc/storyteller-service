package es.vargontoc.storyteller.domain;

import java.util.List;

import es.vargontoc.storyteller.shared.BaseModel;

public class Story  extends BaseModel {
    
    private Long id;
    private String title;
    private List<Character> characters;
    private String summary;
    private List<StoryPage> pages;
    private String promptImage;
    private byte[] image;

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
    public List<Character> getCharacters() {
        return characters;
    }
    public void setCharacters(List<Character> characters) {
        this.characters = characters;
    }
    public String getSummary() {
        return summary;
    }
    public void setSummary(String summary) {
        this.summary = summary;
    }
    public String getPromptImage() {
        return promptImage;
    }
    public void setPromptImage(String promptImage) {
        this.promptImage = promptImage;
    }
    public byte[] getImage() {
        return image;
    }
    public void setImage(byte[] image) {
        this.image = image;
    }
    public List<StoryPage> getPages() {
        return pages;
    }
    public void setPages(List<StoryPage> pages) {
        this.pages = pages;
    }

    
}
