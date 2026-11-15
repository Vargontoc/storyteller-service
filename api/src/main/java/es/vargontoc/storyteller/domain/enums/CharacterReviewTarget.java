package es.vargontoc.storyteller.domain.enums;

public enum CharacterReviewTarget {
    VISUAL, NARRATIVE, BOTH;
    public boolean affectsVisualDescription() { return this == VISUAL || this == BOTH; }
}
