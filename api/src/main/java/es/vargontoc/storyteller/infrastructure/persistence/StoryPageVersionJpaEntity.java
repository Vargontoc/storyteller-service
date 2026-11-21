package es.vargontoc.storyteller.infrastructure.persistence;

import org.hibernate.annotations.ColumnTransformer;

import es.vargontoc.storyteller.domain.enums.PageLengthWarning;
import es.vargontoc.storyteller.domain.model.SceneComposition;
import es.vargontoc.storyteller.infrastructure.converters.SceneCompositionConverter;
import es.vargontoc.storyteller.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "story_page_version")
public class StoryPageVersionJpaEntity extends BaseEntity {

    @JoinColumn(name = "story_page_id", nullable = false)
    @ManyToOne(optional = false)
    private StoryPageJpaEntity page;

    @Column(name = "text", length = 500)
    private String text;

    @Convert(converter = SceneCompositionConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "scene_compose", columnDefinition = "jsonb")
    private SceneComposition composition;

    @Enumerated(EnumType.STRING)
    @Column(name = "length_warning", nullable = false)
    private PageLengthWarning lengthWarning;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    public StoryPageJpaEntity getPage() { return page; }
    public void setPage(StoryPageJpaEntity page) { this.page = page; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public SceneComposition getComposition() { return composition; }
    public void setComposition(SceneComposition composition) { this.composition = composition; }
    public PageLengthWarning getLengthWarning() { return lengthWarning; }
    public void setLengthWarning(PageLengthWarning lengthWarning) { this.lengthWarning = lengthWarning; }
    public boolean isCurrent() { return current; }
    public void setCurrent(boolean current) { this.current = current; }
}
