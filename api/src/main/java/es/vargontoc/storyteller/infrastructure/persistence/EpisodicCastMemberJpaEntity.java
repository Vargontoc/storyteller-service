package es.vargontoc.storyteller.infrastructure.persistence;

import es.vargontoc.storyteller.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Secundario episodico: solo nombre y funcion, sin relacion con el catalogo ni con character. */
@Entity
@Table(name = "episodic_cast_member")
public class EpisodicCastMemberJpaEntity extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "outline_id", nullable = false)
    private StoryOutlineJpaEntity outline;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "role", nullable = false, length = 500)
    private String role;

    public StoryOutlineJpaEntity getOutline() { return outline; }
    public void setOutline(StoryOutlineJpaEntity outline) { this.outline = outline; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
