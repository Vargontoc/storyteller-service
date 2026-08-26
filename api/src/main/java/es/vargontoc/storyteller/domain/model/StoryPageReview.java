package es.vargontoc.storyteller.domain.model;

import es.vargontoc.storyteller.domain.enums.PageReviewTarget;
import es.vargontoc.storyteller.domain.enums.RevisionStatus;

public class StoryPageReview extends StoryPage {
    private long idPage;
    private String hint;
    private boolean hintAccepted;
    private String rejectedReason;
    private RevisionStatus status;
    private PageReviewTarget target;
    private String storySummary;

    public long getIdPage() { return idPage; }
    public void setIdPage(long idPage) { this.idPage = idPage; }
    public String getHint() { return hint; }
    public void setHint(String hint) { this.hint = hint; }
    public boolean isHintAccepted() { return hintAccepted; }
    public void setHintAccepted(boolean hintAccepted) { this.hintAccepted = hintAccepted; }
    public RevisionStatus getStatus() { return status; }
    public void setStatus(RevisionStatus status) { this.status = status; }
    public PageReviewTarget getTarget() { return target; }
    public void setTarget(PageReviewTarget target) { this.target = target;}
    public String getRejectedReason() { return rejectedReason; }
    public void setRejectedReason(String rejectedReason) { this.rejectedReason = rejectedReason; }
    public String getStorySummary() { return storySummary; }
    public void setStorySummary(String storySummary) { this.storySummary = storySummary; }
    
}
