package es.vargontoc.storyteller.domain.model;

import es.vargontoc.storyteller.shared.BaseModel;

public class StoryPage  extends BaseModel {
    
    private Long id;
    private int page;
    private String text;
    private String scene;
    private String imageAsset;
    private String audioAsset;
    
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public int getPage() {
        return page;
    }
    public void setPage(int page) {
        this.page = page;
    }
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
    public String getScene() {
        return scene;
    }
    public void setScene(String scene) {
        this.scene = scene;
    }
    public String getImageAsset() {
        return imageAsset;
    }
    public void setImageAsset(String imageAsset) {
        this.imageAsset = imageAsset;
    }
    public String getAudioAsset() {
        return audioAsset;
    }
    public void setAudioAsset(String audioAsset) {
        this.audioAsset = audioAsset;
    }

    
    

    
}
