package es.vargontoc.storyteller.infrastructure.persistence;

import es.vargontoc.storyteller.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Audio de una pagina interior (page >= 1): ruta del fichero, como el audio de las paginas antiguas. */
@Entity
@Table(name = "story_narration_audio")
public class StoryNarrationAudioJpaEntity extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "narration_id", nullable = false)
    private StoryNarrationJpaEntity narration;

    @Column(name = "page", nullable = false)
    private int page;

    @Column(name = "audio_asset", nullable = false)
    private String audioAsset;

    public StoryNarrationJpaEntity getNarration() { return narration; }
    public void setNarration(StoryNarrationJpaEntity narration) { this.narration = narration; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public String getAudioAsset() { return audioAsset; }
    public void setAudioAsset(String audioAsset) { this.audioAsset = audioAsset; }
}
