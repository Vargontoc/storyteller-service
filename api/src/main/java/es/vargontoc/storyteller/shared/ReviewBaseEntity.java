package es.vargontoc.storyteller.shared;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import es.vargontoc.storyteller.domain.model.RevisionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class ReviewBaseEntity extends BaseEntity {
    
    @Column(nullable = false, length = 500)
    private String hint;

    @Column(name = "hint_accepted", nullable = false)
    private boolean hintAccepted;

    @Column(name = "rejected_reason", length = 500)
    private String rejectedReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RevisionStatus status;

    
    public String getHint() { return hint; }
    public void setHint(String hint) { this.hint = hint; }
    public boolean isHintAccepted() { return hintAccepted; }
    public void setHintAccepted(boolean hintAccepted) { this.hintAccepted = hintAccepted; }
    public String getRejectedReason() { return rejectedReason; }
    public void setRejectedReason(String rejectedReason) { this.rejectedReason = rejectedReason; }
    public RevisionStatus getStatus() { return status; }
    public void setStatus(RevisionStatus status) { this.status = status; }

    public void markAcepted() { this.status = RevisionStatus.CONFIRMED; }
}
