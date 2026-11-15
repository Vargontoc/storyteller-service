package es.vargontoc.storyteller.domain.response;
public record CatalogCharacterReviewRequestResult(boolean replacementConfirmationRequired, boolean visualConsistencyWarning, CatalogCharacterReviewAgentResult proposal) {
    public static CatalogCharacterReviewRequestResult confirmationRequired() { return new CatalogCharacterReviewRequestResult(true, false, null); }
    public static CatalogCharacterReviewRequestResult proposal(CatalogCharacterReviewAgentResult proposal, boolean visualConsistencyWarning) { return new CatalogCharacterReviewRequestResult(false, visualConsistencyWarning, proposal); }
}
