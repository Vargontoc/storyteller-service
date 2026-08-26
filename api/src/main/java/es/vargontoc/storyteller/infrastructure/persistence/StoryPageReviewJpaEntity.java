package es.vargontoc.storyteller.infrastructure.persistence;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnTransformer;

import es.vargontoc.storyteller.domain.enums.PageReviewTarget;
import es.vargontoc.storyteller.domain.enums.RevisionStatus;
import es.vargontoc.storyteller.domain.model.SceneComposition;
import es.vargontoc.storyteller.infrastructure.converters.SceneCompositionConverter;
import es.vargontoc.storyteller.shared.ReviewBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "story_page_review")
public class StoryPageReviewJpaEntity extends ReviewBaseEntity{
    
    @JoinColumn(name = "story_page_id", nullable = false)
    @ManyToOne(optional = false)
    private StoryPageJpaEntity page;

    @Enumerated(EnumType.STRING)
    @Column(name = "target", nullable = false)
    private PageReviewTarget target;

    @Column(name = "candidate_text", nullable = false, length = 500)
    private String text;

    @Convert(converter =  SceneCompositionConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "scene_compose", columnDefinition = "jsonb")
    private SceneComposition composition;

    public StoryPageJpaEntity getPage() { return page; }
    public void setPage(StoryPageJpaEntity page) { this.page = page; }
    public PageReviewTarget getTarget() { return target; }
    public void setTarget(PageReviewTarget target) { this.target = target; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public SceneComposition getComposition() { return composition; }
    public void setComposition(SceneComposition composition) { this.composition = composition; }

    public static StoryPageReviewJpaEntity candidate(StoryPageJpaEntity page,
        String hint,
        boolean hintAccepted,
        String rejectionReason,
        PageReviewTarget targetPage,
        String text) {
        StoryPageReviewJpaEntity target = new StoryPageReviewJpaEntity();
        target.setPage(page);
        target.setText(text);
        target.setHint(hint);
        target.setRejectedReason(rejectionReason);
        target.setTarget(targetPage);

        if(hintAccepted)
            target.setStatus(RevisionStatus.PENDING);
        else
            target.setStatus(RevisionStatus.DISCARDED);


        target.setCreatedAt(LocalDateTime.now());

        return target;
    }

    

}
