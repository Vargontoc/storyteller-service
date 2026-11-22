package es.vargontoc.storyteller.infrastructure.persistence;

import java.util.ArrayList;
import java.util.List;

import es.vargontoc.storyteller.domain.enums.NarrationStatus;
import es.vargontoc.storyteller.shared.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/** Narracion de un cuento (cero o una por cuento). La voz es una copia del nombre, sin clave a la voz subida. */
@Entity
@Table(name = "story_narration")
public class StoryNarrationJpaEntity extends BaseEntity {

    @OneToOne(optional = false)
    @JoinColumn(name = "story_id", nullable = false, unique = true)
    private StoryJpaEntity story;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NarrationStatus status;

    @Column(name = "voice_name", length = 100)
    private String voiceName;

    @OneToMany(mappedBy = "narration", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("page ASC")
    private List<StoryNarrationAudioJpaEntity> audios = new ArrayList<>();

    public StoryJpaEntity getStory() { return story; }
    public void setStory(StoryJpaEntity story) { this.story = story; }
    public NarrationStatus getStatus() { return status; }
    public void setStatus(NarrationStatus status) { this.status = status; }
    public String getVoiceName() { return voiceName; }
    public void setVoiceName(String voiceName) { this.voiceName = voiceName; }
    public List<StoryNarrationAudioJpaEntity> getAudios() { return audios; }
    public void setAudios(List<StoryNarrationAudioJpaEntity> audios) { this.audios = audios; }
}
