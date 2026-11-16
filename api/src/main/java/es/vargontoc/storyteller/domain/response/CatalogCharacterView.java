package es.vargontoc.storyteller.domain.response;
import es.vargontoc.storyteller.domain.enums.CatalogPortraitStatus;
public record CatalogCharacterView(Long id, String name, String narrativeDescription, String visualDescription, Boolean draft, CatalogPortraitStatus portraitStatus, boolean pendingReview) {}
