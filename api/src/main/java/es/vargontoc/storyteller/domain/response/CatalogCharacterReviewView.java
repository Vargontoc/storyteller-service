package es.vargontoc.storyteller.domain.response;
import es.vargontoc.storyteller.domain.enums.CharacterReviewTarget;
public record CatalogCharacterReviewView(Long id, CharacterReviewTarget target, String hint, String name,
        String narrativeDescription, String visualDescription, boolean forkConfirmationRequired, Long forkCatalogCharacterId) {
    public CatalogCharacterReviewView(Long id, CharacterReviewTarget target, String hint, String name,
            String narrativeDescription, String visualDescription) {
        this(id, target, hint, name, narrativeDescription, visualDescription, false, null);
    }
    public CatalogCharacterReviewView(Long id, CharacterReviewTarget target, String hint, String name,
            String narrativeDescription, String visualDescription, boolean forkConfirmationRequired) {
        this(id, target, hint, name, narrativeDescription, visualDescription, forkConfirmationRequired, null);
    }
}
