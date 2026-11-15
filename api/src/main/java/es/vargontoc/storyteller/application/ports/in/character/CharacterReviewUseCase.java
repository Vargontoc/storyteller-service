package es.vargontoc.storyteller.application.ports.in.character;
import es.vargontoc.storyteller.domain.command.CatalogCharacterReviewCommand;
import es.vargontoc.storyteller.domain.response.CatalogCharacterReviewView;
import es.vargontoc.storyteller.domain.response.CatalogCharacterReviewRequestResult;
public interface CharacterReviewUseCase { CatalogCharacterReviewRequestResult request(CatalogCharacterReviewCommand command); CatalogCharacterReviewView getPending(Long catalogCharacterId); }
