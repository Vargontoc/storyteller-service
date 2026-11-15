package es.vargontoc.storyteller.domain.response;
import es.vargontoc.storyteller.domain.enums.CharacterReviewTarget;
public record CatalogCharacterReviewView(Long id, CharacterReviewTarget target, String hint, String name, String narrativeDescription, String visualDescription) {}
