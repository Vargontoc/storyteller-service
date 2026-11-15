package es.vargontoc.storyteller.application.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import es.vargontoc.storyteller.application.ports.out.external.CharacterAgent;
import es.vargontoc.storyteller.application.ports.out.persistence.CatalogCharacterRepository;
import es.vargontoc.storyteller.domain.command.CatalogCharacterCreateCommand;
import es.vargontoc.storyteller.domain.response.CatalogCharacterAgentResult;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import es.vargontoc.storyteller.infrastructure.validations.CatalogCharacterAgentResultValidator;
class CharacterCatalogServiceTest { private final CatalogCharacterAgentResultValidator validator=new CatalogCharacterAgentResultValidator();
 @Test void acceptedAutomaticResultIsPersisted(){ CharacterAgent a=mock(CharacterAgent.class); CatalogCharacterRepository r=mock(CatalogCharacterRepository.class); when(a.generateAutomatically()).thenReturn(ok()); new CharacterCatalogService(a,r,validator,id -> false, java.util.Optional.empty()).create(new CatalogCharacterCreateCommand(null)); verify(r).create(any()); }
 @Test void acceptedManualResultIsPersisted(){ CharacterAgent a=mock(CharacterAgent.class); CatalogCharacterRepository r=mock(CatalogCharacterRepository.class); when(a.generateFromHint("zorra")).thenReturn(ok()); new CharacterCatalogService(a,r,validator,id -> false, java.util.Optional.empty()).create(new CatalogCharacterCreateCommand("zorra")); verify(r).create(any()); }
 @Test void rejectedResultIsNotPersisted(){ CharacterAgent a=mock(CharacterAgent.class); CatalogCharacterRepository r=mock(CatalogCharacterRepository.class); when(a.generateAutomatically()).thenReturn(new CatalogCharacterAgentResult(false,"no apto",null,null,null,null,null)); var out=new CharacterCatalogService(a,r,validator,id -> false, java.util.Optional.empty()).create(new CatalogCharacterCreateCommand(null)); assertFalse(out.accepted()); verifyNoInteractions(r); }
 @Test void incompleteAcceptedResultIsNotPersisted(){ CharacterAgent a=mock(CharacterAgent.class); CatalogCharacterRepository r=mock(CatalogCharacterRepository.class); when(a.generateAutomatically()).thenReturn(new CatalogCharacterAgentResult(true,null,"Luna",null,"zorra",null,List.of())); assertThrows(AppException.class,()->new CharacterCatalogService(a,r,validator,id -> false, java.util.Optional.empty()).create(new CatalogCharacterCreateCommand(null))); verifyNoInteractions(r); }
 private CatalogCharacterAgentResult ok(){return new CatalogCharacterAgentResult(true,null,"Luna","curiosa","zorra","fox",List.of());}
}
