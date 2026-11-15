package es.vargontoc.storyteller.domain.response;
public record CatalogCharacterView(Long id, String name, String narrativeDescription, String visualDescription, Boolean draft, boolean pendingReview) {}
