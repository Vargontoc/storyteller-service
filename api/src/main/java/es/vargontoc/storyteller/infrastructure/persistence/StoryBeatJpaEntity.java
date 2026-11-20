package es.vargontoc.storyteller.infrastructure.persistence;

import es.vargontoc.storyteller.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Beat de la escaleta: uno por pagina interior, con sus cinco campos. */
@Entity
@Table(name = "story_beat")
public class StoryBeatJpaEntity extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "outline_id", nullable = false)
    private StoryOutlineJpaEntity outline;

    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    @Column(name = "event", nullable = false, length = 1000)
    private String event;

    @Column(name = "narrative_function", nullable = false, length = 500)
    private String narrativeFunction;

    /** Nombres separados por salto de linea. */
    @Column(name = "participants", nullable = false, length = 1000)
    private String participants;

    @Column(name = "tone", nullable = false, length = 300)
    private String tone;

    @Column(name = "visual_note", nullable = false, length = 1000)
    private String visualNote;

    public StoryOutlineJpaEntity getOutline() { return outline; }
    public void setOutline(StoryOutlineJpaEntity outline) { this.outline = outline; }
    public int getPageNumber() { return pageNumber; }
    public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }
    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }
    public String getNarrativeFunction() { return narrativeFunction; }
    public void setNarrativeFunction(String narrativeFunction) { this.narrativeFunction = narrativeFunction; }
    public String getParticipants() { return participants; }
    public void setParticipants(String participants) { this.participants = participants; }
    public String getTone() { return tone; }
    public void setTone(String tone) { this.tone = tone; }
    public String getVisualNote() { return visualNote; }
    public void setVisualNote(String visualNote) { this.visualNote = visualNote; }
}
