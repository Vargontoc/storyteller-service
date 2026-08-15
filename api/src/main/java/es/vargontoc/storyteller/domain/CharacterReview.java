package es.vargontoc.storyteller.domain;

public class CharacterReview extends CharacterModel {

    private Long characterId;
    private String hint;
    private boolean hintAccepted;
    private String rejectedReason;
    private RevisionStatus status;
    private CharacterReviewTarget target;

    public Long getCharacterId() {
        return characterId;
    }
    public void setCharacterId(Long characterId) {
        this.characterId = characterId;
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
    public CharacterReviewTarget getTarget() {
        return target;
    }
    public void setTarget(CharacterReviewTarget target) {
        this.target = target;
    }
}
