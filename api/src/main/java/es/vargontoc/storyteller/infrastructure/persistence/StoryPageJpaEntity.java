package es.vargontoc.storyteller.infrastructure.persistence;

import org.hibernate.annotations.ColumnTransformer;

import es.vargontoc.storyteller.domain.model.SceneComposition;
import es.vargontoc.storyteller.infrastructure.converters.SceneCompositionConverter;
import es.vargontoc.storyteller.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "story_page")
public class StoryPageJpaEntity extends BaseEntity 
{
    @JoinColumn(name = "story_id", nullable = false)
    @ManyToOne(optional = false)
    private StoryJpaEntity story;

    @Column(name = "page", nullable = false)
    private int page;

    @Column(name = "text", nullable =   false, length = 500)
    private String text;

    @Column(name = "image_asset")
    private String imageAsset;

    @Column(name = "audio_asset")
    private String audioAsset;

    @Convert(converter =  SceneCompositionConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "scene_compose", columnDefinition = "jsonb")
    private SceneComposition composition;

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getImageAsset() { return imageAsset;}
    public void setImageAsset(String imageAsset) { this.imageAsset = imageAsset; }
    public String getAudioAsset() { return audioAsset; }
    public void setAudioAsset(String audioAsset) { this.audioAsset = audioAsset; }
    public StoryJpaEntity getStory() { return story; }
    public void setStory(StoryJpaEntity story) { this.story = story; }
    public SceneComposition getSceneComposition() { return composition; }
    public void setSceneComposition(SceneComposition metadata) { this.composition = metadata; }

    
}
