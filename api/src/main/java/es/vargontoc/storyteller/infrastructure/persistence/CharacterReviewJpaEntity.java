package es.vargontoc.storyteller.infrastructure.persistence;

import java.time.LocalDateTime;

import es.vargontoc.storyteller.domain.enums.CharacterReviewTarget;
import es.vargontoc.storyteller.domain.enums.RevisionStatus;
import es.vargontoc.storyteller.shared.ReviewBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "character_review")
public class CharacterReviewJpaEntity extends ReviewBaseEntity {
    
    public CharacterReviewJpaEntity() { super(); }

    @ManyToOne(optional = false)
    private CharacterJpaEntity character;

    @Enumerated(EnumType.STRING)
    @Column(name = "target", nullable = false)
    private CharacterReviewTarget target;

    @Column(name = "candidate_narrative", length = 1000)
    private String candidateNarrative;

    @Column(name = "candidate_visual", length = 1000)
    private String candidateVisual;

    public CharacterJpaEntity getCharacter() { return character; }
    public void setCharacter(CharacterJpaEntity character) { this.character = character; }
    public CharacterReviewTarget getTarget() { return target; }
    public void setTarget(CharacterReviewTarget target) { this.target = target; }
    public String getCandidateNarrative() { return candidateNarrative; }
    public void setCandidateNarrative(String candidateNarrative) { this.candidateNarrative = candidateNarrative; }
    public String getCandidateVisual() { return candidateVisual; }
    public void setCandidateVisual(String candidateVisual) { this.candidateVisual = candidateVisual; }

    public static CharacterReviewJpaEntity candidate(
        CharacterJpaEntity character,
        String hint,
        CharacterReviewTarget target,
        boolean hintAccepted,
        String rejectedReason,
        String narrative,
        String visual) {
            CharacterReviewJpaEntity result = new CharacterReviewJpaEntity();
            result.setCharacter(character);
            result.setHint(hint);
            result.setTarget(target);
            result.setHintAccepted(hintAccepted);
            result.setRejectedReason(rejectedReason);
            result.setCandidateNarrative(narrative);
            result.setCandidateVisual(visual);
            result.setCreatedAt(LocalDateTime.now());
            if(hintAccepted)
                result.setStatus(RevisionStatus.PENDING);
            else
                result.setStatus(RevisionStatus.DISCARDED);
            return result;
        }
}