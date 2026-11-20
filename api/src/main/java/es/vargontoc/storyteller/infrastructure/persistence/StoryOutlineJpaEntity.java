package es.vargontoc.storyteller.infrastructure.persistence;

import java.util.ArrayList;
import java.util.List;

import es.vargontoc.storyteller.shared.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/** Escaleta de un cuento nuevo: beats por pagina y reparto ligero. Su ausencia indica cuento antiguo. */
@Entity
@Table(name = "story_outline")
public class StoryOutlineJpaEntity extends BaseEntity {

    @OneToOne(optional = false)
    @JoinColumn(name = "story_id", nullable = false, unique = true)
    private StoryJpaEntity story;

    @OneToMany(mappedBy = "outline", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("pageNumber ASC")
    private List<StoryBeatJpaEntity> beats = new ArrayList<>();

    @OneToMany(mappedBy = "outline", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<EpisodicCastMemberJpaEntity> cast = new ArrayList<>();

    public StoryJpaEntity getStory() { return story; }
    public void setStory(StoryJpaEntity story) { this.story = story; }
    public List<StoryBeatJpaEntity> getBeats() { return beats; }
    public void setBeats(List<StoryBeatJpaEntity> beats) { this.beats = beats; }
    public List<EpisodicCastMemberJpaEntity> getCast() { return cast; }
    public void setCast(List<EpisodicCastMemberJpaEntity> cast) { this.cast = cast; }
}
