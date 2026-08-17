package es.vargontoc.storyteller.domain.model;

import es.vargontoc.storyteller.domain.enums.RevisionStatus;
import es.vargontoc.storyteller.shared.BaseModel;

public class StoryReview extends BaseModel {
    
    private Long id;
    private Story previewStory;
    private String hint;
    private boolean hintAccepted;
    private String rejectedReason;
    private RevisionStatus status;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Story getPreviewStory() {
        return previewStory;
    }
    public void setPreviewStory(Story previewStory) {
        this.previewStory = previewStory;
    }
    public String getHint() {
        return hint;
    }
    public void setHint(String hint) {
        this.hint = hint;
    }
    public boolean isHintAccepted() {
        return hintAccepted;
    }
    public void setHintAccepted(boolean hintAccepted) {
        this.hintAccepted = hintAccepted;
    }
    public String getRejectedReason() {
        return rejectedReason;
    }
    public void setRejectedReason(String rejectedReason) {
        this.rejectedReason = rejectedReason;
    }
    public RevisionStatus getStatus() {
        return status;
    }
    public void setStatus(RevisionStatus status) {
        this.status = status;
    }

    

}
