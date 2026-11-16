package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import es.vargontoc.storyteller.application.ports.in.character.CharacterReviewUseCase;
import es.vargontoc.storyteller.domain.enums.CharacterReviewTarget;
import es.vargontoc.storyteller.domain.response.CatalogCharacterReviewRequestResult;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto.ReviewCharacterRequestDto;

class CharacterReviewControllerTest {
 @Test void postsReviewRequestWithConfirmationFlag(){
  CharacterReviewUseCase useCase=mock(CharacterReviewUseCase.class); when(useCase.request(any())).thenReturn(CatalogCharacterReviewRequestResult.confirmationRequired());
  var response=new CharacterReviewController(useCase).request(12L,new ReviewCharacterRequestDto(CharacterReviewTarget.VISUAL,"más alegre",true));
  assertTrue(response.getBody().success()); assertTrue(response.getBody().data().replacementConfirmationRequired()); verify(useCase).request(argThat(command->command.catalogCharacterId().equals(12L)&&command.replacePending()));
 }
}
