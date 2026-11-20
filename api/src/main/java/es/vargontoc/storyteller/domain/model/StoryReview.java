package es.vargontoc.storyteller.domain.model;

import java.util.List;

import es.vargontoc.storyteller.domain.enums.RevisionStatus;
import es.vargontoc.storyteller.shared.BaseModel;

public class StoryReview extends BaseModel {
    
    private Long id;
    private Story previewStory;
    private String hint;
    private boolean hintAccepted;
    private String rejectedReason;
    private RevisionStatus status;
    /** Escaleta candidata completa; null si la revision no propone escaleta (hint rechazado o revision antigua). */
    private StoryOutline candidateOutline;
    /** Numeros de pagina (desde 1) cuyo beat es realmente distinto del vigente. */
    private List<Integer> changedBeats = List.of();

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
    public StoryOutline getCandidateOutline() {
        return candidateOutline;
    }
    public void setCandidateOutline(StoryOutline candidateOutline) {
        this.candidateOutline = candidateOutline;
    }
    public List<Integer> getChangedBeats() {
        return changedBeats;
    }
    public void setChangedBeats(List<Integer> changedBeats) {
        this.changedBeats = changedBeats;
    }
}
