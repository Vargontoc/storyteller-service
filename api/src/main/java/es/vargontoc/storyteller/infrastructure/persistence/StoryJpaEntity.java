package es.vargontoc.storyteller.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import es.vargontoc.storyteller.domain.enums.StorySize;
import es.vargontoc.storyteller.domain.enums.StoryStatus;
import es.vargontoc.storyteller.shared.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "story")
public class StoryJpaEntity extends BaseEntity {
    
    public StoryJpaEntity() { super(); }

    @Column(name = "title", nullable = false, length = 50)
    private String title;

    @JoinColumn(name = "topic_id", nullable = false)
    @ManyToOne(optional = false)
    private TopicJpaEntity topic;
    
    @Column(name = "synopsis", nullable = false, length = 500)
    private String synopsis;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "size", nullable = false)
    private StorySize size;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StoryStatus status;
    
    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CharacterJpaEntity> characters = new ArrayList<>();

    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StoryPageJpaEntity> pages = new ArrayList<>();

    @Transient
    private Long reviewId;


    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public StorySize getSize() {
        return size;
    }
    public void setSize(StorySize size) {
        this.size = size;
    }
    public TopicJpaEntity getTopic() {
        return topic;
    }
    public void setTopic(TopicJpaEntity topic) {
        this.topic = topic;
    }
    public String getSynopsis() {
        return synopsis;
    }
    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }
    public StoryStatus getStatus() {
        return status;
    }
    public void setStatus(StoryStatus status) {
        this.status = status;
    }
    public List<CharacterJpaEntity> getCharacters() {
        return characters;
    }
    public void setCharacters(List<CharacterJpaEntity> characters) {
        this.characters = characters;
    }
    public Long getReviewId() {
        return reviewId;
    }
    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }
    
    public List<StoryPageJpaEntity> getPages() {
        return pages;
    }
    public void setPages(List<StoryPageJpaEntity> pages) {
        this.pages = pages;
    }
    

    public static StoryJpaEntity draft(TopicJpaEntity topic, StorySize size, String title, String synopsis) {
        StoryJpaEntity story = new StoryJpaEntity();
        story.setTopic(topic);
        story.setSize(size);
        story.setTitle(title);
        story.setSynopsis(synopsis);
        story.setStatus(StoryStatus.SCRIPT_PENDING);
        story.setCreatedAt(LocalDateTime.now());
        return story;
    }

}
