package es.vargontoc.storyteller.domain.model;

import es.vargontoc.storyteller.shared.BaseModel;

public class StoryPage  extends BaseModel {
    
    private Long id;
    private String content;
    private String imagePrompt;
    private byte[] image;
    private byte[] audio;
    
    public Long getId()  {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public String getImagePrompt() {
        return imagePrompt;
    }
    public void setImagePrompt(String imagePrompt) {
        this.imagePrompt = imagePrompt;
    }
    public byte[] getImage() {
        return image;
    }
    public void setImage(byte[] image) {
        this.image = image;
    }
    public byte[] getAudio() {
        return audio;
    }
    public void setAudio(byte[] audio) {
        this.audio = audio;
    }

    
}
