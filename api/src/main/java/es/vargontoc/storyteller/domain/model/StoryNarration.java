package es.vargontoc.storyteller.domain.model;

import java.util.List;

import es.vargontoc.storyteller.domain.enums.NarrationStatus;
import es.vargontoc.storyteller.shared.BaseModel;

/**
 * Narracion de un cuento: estado, copia del nombre de la voz usada (sin vinculo con la voz subida)
 * y numeros de las paginas interiores con audio (nunca la portada, pagina 0).
 */
public class StoryNarration extends BaseModel {

    private Long id;
    private final Long storyId;
    private final NarrationStatus status;
    private final String voiceName;
    private final List<Integer> audioPages;

    public StoryNarration(Long storyId, NarrationStatus status, String voiceName, List<Integer> audioPages) {
        this.storyId = storyId;
        this.status = status;
        this.voiceName = voiceName;
        this.audioPages = audioPages;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStoryId() { return storyId; }
    public NarrationStatus getStatus() { return status; }
    /** Nombre de la voz con la que se narro; null mientras no hay audio publicado. */
    public String getVoiceName() { return voiceName; }
    public List<Integer> getAudioPages() { return audioPages; }
    public boolean hasAudio() { return !audioPages.isEmpty(); }
}
