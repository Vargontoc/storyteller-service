package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;
import static org.junit.jupiter.api.Assertions.*; import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import es.vargontoc.storyteller.application.ports.in.character.CharacterCatalogUseCase;
import es.vargontoc.storyteller.domain.response.CatalogCharacterAgentResult;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto.CatalogCharacterCreateRequest;
class CharacterCatalogControllerTest {
 @Test void delegatesManualCreate(){ CharacterCatalogUseCase useCase=mock(CharacterCatalogUseCase.class); when(useCase.create(any())).thenReturn(new CatalogCharacterAgentResult(false,"no apto",null,null,null,null,null)); var response=new CharacterCatalogController(useCase).create(new CatalogCharacterCreateRequest("texto")); assertTrue(response.getBody().success()); assertFalse(response.getBody().data().accepted()); verify(useCase).create(argThat(c->"texto".equals(c.hint()))); }
}
