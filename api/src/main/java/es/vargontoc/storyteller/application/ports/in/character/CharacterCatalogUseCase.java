package es.vargontoc.storyteller.application.ports.in.character;

import es.vargontoc.storyteller.domain.command.CatalogCharacterCreateCommand;
import es.vargontoc.storyteller.domain.model.CatalogCharacter;
import es.vargontoc.storyteller.domain.response.CatalogCharacterAgentResult;
import es.vargontoc.storyteller.domain.response.CatalogCharacterView;
import java.util.List;
import es.vargontoc.storyteller.domain.model.PortraitCandidate;

public interface CharacterCatalogUseCase {
    CatalogCharacterAgentResult create(CatalogCharacterCreateCommand command);
    CatalogCharacter persistAccepted(CatalogCharacterAgentResult result);
    List<CatalogCharacterView> list();
    CatalogCharacterView get(Long id);
    CatalogCharacterView rename(Long id, String name);
    void generatePortraits(Long id);
    List<PortraitCandidate> candidates(Long id);
    void selectPortrait(Long id, Long candidateId);
    boolean generatingPortraits(Long id);
    byte[] candidateAsset(Long id, Long candidateId);
    byte[] approvedPortraitAsset(Long id);
}
