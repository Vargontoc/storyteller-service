package es.vargontoc.storyteller.infrastructure.persistence;

import es.vargontoc.storyteller.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "story_page")
public class PageJpaEntity extends BaseEntity 
{
    @JoinColumn(name = "story_id", nullable = false)
    @ManyToOne(optional = false)
    private StoryJpaEntity story;

    @Column(name = "page", nullable = false)
    private int page;

    @Column(name = "is_cover", nullable = false)
    private boolean isCover;

    @Column(name = "is_last_page", nullable = false)
    private boolean isLastPage;

    @Column(name = "text", nullable =   false, length = 500)
    private String text;

    @Column(name = "scene_prompt", nullable = false, length = 2000)
    private String scenePrompt;

    @Column(name = "image_asset")
    private String imageAsset;

    @Column(name = "audio_asset")
    private String audioAsset;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public boolean isCover() {
        return isCover;
    }

    public void setCover(boolean isCover) {
        this.isCover = isCover;
    }

    public boolean isLastPage() {
        return isLastPage;
    }

    public void setLastPage(boolean isLastPage) {
        this.isLastPage = isLastPage;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getScenePrompt() {
        return scenePrompt;
    }

    public void setScenePrompt(String scenePrompt) {
        this.scenePrompt = scenePrompt;
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

    public StoryJpaEntity getStory() {
        return story;
    }

    public void setStory(StoryJpaEntity story) {
        this.story = story;
    }

    
}
