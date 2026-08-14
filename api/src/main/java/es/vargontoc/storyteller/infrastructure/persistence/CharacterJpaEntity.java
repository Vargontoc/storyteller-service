package es.vargontoc.storyteller.infrastructure.persistence;

import es.vargontoc.storyteller.domain.CharacterReviewTarget;
import es.vargontoc.storyteller.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "character")
public class CharacterJpaEntity extends BaseEntity {
    
    public CharacterJpaEntity() { super(); }

    @Column(name = "name", nullable = false, length = 20)
    private String name;

    @JoinColumn(name = "story_id", nullable = false)
    @ManyToOne(optional = false)
    private StoryJpaEntity story;

    @Column(name = "visual_description", nullable = false, length = 1000)
    private String visualDescription;

    @Column(name = "narrative_description", nullable = false, length = 1000)
    private String narrativeDescription;
    
    @Transient
    private Long reviewId;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    public String getVisualDescription() {
        return visualDescription;
    }
    public void setVisualDescription(String visualDescription) {
        this.visualDescription = visualDescription;
    }
    public String getNarrativeDescription() {
        return narrativeDescription;
    }
    public void setNarrativeDescription(String narrativeDescription) {
        this.narrativeDescription = narrativeDescription;
    }
    public StoryJpaEntity getStory() {
        return story;
    }
    public void setStory(StoryJpaEntity story) {
        this.story = story;
    }
    public Long getReviewId() {
        return reviewId;
    }
    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public static CharacterJpaEntity draft(String name, String visual, String narrative){
        CharacterJpaEntity c = new CharacterJpaEntity();
        c.setName(name);
        c.setNarrativeDescription(narrative);
        c.setVisualDescription(visual);
        return c;
    }

    public void replaceContent(CharacterReviewTarget target, String narrative, String visual){
        switch (target) {
            case BOTH:
                setNarrativeDescription(narrative);
                setVisualDescription(visual);
                break;
            case NARRATIVE:
                setNarrativeDescription(narrative);
                break;
            case VISUAL:
                setVisualDescription(visual);
                break;
        }
    }

    void assignTo(StoryJpaEntity story) { this.story = story; }
}
