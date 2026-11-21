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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "story")
public class StoryJpaEntity extends BaseEntity {
    
    public StoryJpaEntity() { super(); }

    @Column(name = "title", nullable = false, length = 50)
    private String title;

    /** Tema con el que nació el cuento; es una instantánea textual, no una FK. */
    @Column(name = "topic", nullable = false, length = 30)
    private String topic;

    @Column(name = "topic_description", nullable = false, length = 100)
    private String topicDescription;
    
    @Column(name = "synopsis", nullable = false, length = 500)
    private String synopsis;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "size", nullable = false)
    private StorySize size;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StoryStatus status;
    
    @Column(name = "summary", length = 1000)
    private String summary;

    /** Version de guion; null en cuentos antiguos (sin escaleta). */
    @Column(name = "script_version")
    private Integer scriptVersion;

    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CharacterJpaEntity> characters = new ArrayList<>();

    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StoryPageJpaEntity> pages = new ArrayList<>();

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public StorySize getSize() { return size; }
    public void setSize(StorySize size) { this.size = size;}
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getTopicDescription() { return topicDescription; }
    public void setTopicDescription(String topicDescription) { this.topicDescription = topicDescription; }
    public String getSynopsis() { return synopsis; }
    public void setSynopsis(String synopsis) { this.synopsis = synopsis; }
    public StoryStatus getStatus() {  return status; }
    public void setStatus(StoryStatus status) { this.status = status; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Integer getScriptVersion() { return scriptVersion; }
    public void setScriptVersion(Integer scriptVersion) { this.scriptVersion = scriptVersion; }
    public List<CharacterJpaEntity> getCharacters() { return characters;}
    public void setCharacters(List<CharacterJpaEntity> characters) { this.characters = characters; }
    public List<StoryPageJpaEntity> getPages() { return pages; }
    public void setPages(List<StoryPageJpaEntity> pages) { this.pages = pages; }
    

    public static StoryJpaEntity draft(StorySize size, String topic, String topicDescription, String title, String synopsis) {
        StoryJpaEntity story = new StoryJpaEntity();
        story.setTopic(topic);
        story.setTopicDescription(topicDescription);
        story.setSize(size);
        story.setTitle(title);
        story.setSynopsis(synopsis);
        story.setStatus(StoryStatus.SCRIPT_PENDING);
        story.setCreatedAt(LocalDateTime.now());
        return story;
    }
}
