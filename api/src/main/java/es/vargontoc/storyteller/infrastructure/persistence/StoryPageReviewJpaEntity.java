package es.vargontoc.storyteller.infrastructure.persistence;

import es.vargontoc.storyteller.domain.model.PageReviewTarget;
import es.vargontoc.storyteller.shared.ReviewBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "story_page_review")
public class StoryPageReviewJpaEntity extends ReviewBaseEntity{
    
    @ManyToOne(optional = false)
    private PageJpaEntity page;

    @Enumerated(EnumType.STRING)
    @Column(name = "target", nullable = false)
    private PageReviewTarget target;

    @Column(name = "text", nullable = false, length = 500)
    private String text;

    @Column(name = "prompt_scene", nullable = false, length = 2000)
    private String scene;

}
