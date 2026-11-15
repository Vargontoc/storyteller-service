package es.vargontoc.storyteller.domain.command;
import es.vargontoc.storyteller.domain.enums.CharacterReviewTarget;
public record CatalogCharacterReviewCommand(Long catalogCharacterId, CharacterReviewTarget target, String hint, boolean replacePending) {}
