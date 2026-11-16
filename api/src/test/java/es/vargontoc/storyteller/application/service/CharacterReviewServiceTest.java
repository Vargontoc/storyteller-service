package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import es.vargontoc.storyteller.application.ports.out.external.CharacterAgent;
import es.vargontoc.storyteller.application.ports.out.external.CharacterImageConditionPort;
import es.vargontoc.storyteller.domain.command.CatalogCharacterReviewCommand;
import es.vargontoc.storyteller.domain.enums.CharacterReviewTarget;
import es.vargontoc.storyteller.domain.enums.RevisionStatus;
import es.vargontoc.storyteller.domain.model.ActorMetadata;
import es.vargontoc.storyteller.domain.response.CatalogCharacterReviewAgentResult;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterReviewJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterReviewJpaRepository;

class CharacterReviewServiceTest {
 @Test void createsPendingProposalWhenThereIsNoPreviousReview(){
  Fixture fixture=new Fixture(Optional.empty(),Optional.of(mock(CharacterImageConditionPort.class))); when(fixture.agent.review(any())).thenReturn(proposal("nuevo visual"));
  var result=fixture.service.request(command(CharacterReviewTarget.NARRATIVE,false));
  assertFalse(result.replacementConfirmationRequired()); assertFalse(result.visualConsistencyWarning()); assertTrue(result.proposal().accepted()); verify(fixture.reviews).save(any(CatalogCharacterReviewJpaEntity.class)); verifyNoInteractions(fixture.images.get());
 }
 @Test void pendingReviewRequiresConfirmationWithoutCallingAgent(){
  CatalogCharacterReviewJpaEntity previous=new CatalogCharacterReviewJpaEntity(); Fixture fixture=new Fixture(Optional.of(previous),Optional.empty());
  var result=fixture.service.request(command(CharacterReviewTarget.NARRATIVE,false));
  assertTrue(result.replacementConfirmationRequired()); assertNull(result.proposal()); verifyNoInteractions(fixture.agent); verify(fixture.reviews,never()).saveAndFlush(any());
 }
 @Test void confirmedReplacementDiscardsPreviousBeforeProcessingAndRejectsWithoutNewPending(){
  CatalogCharacterReviewJpaEntity previous=new CatalogCharacterReviewJpaEntity(); Fixture fixture=new Fixture(Optional.of(previous),Optional.empty()); when(fixture.agent.review(any())).thenReturn(new CatalogCharacterReviewAgentResult(false,"no apto",null,null,null,null,null));
  var result=fixture.service.request(command(CharacterReviewTarget.NARRATIVE,true));
  assertFalse(result.proposal().accepted()); assertEquals(RevisionStatus.DISCARDED,previous.getStatus()); InOrder order=inOrder(fixture.reviews,fixture.agent); order.verify(fixture.reviews).saveAndFlush(previous); order.verify(fixture.agent).review(any()); verify(fixture.reviews,never()).save(any(CatalogCharacterReviewJpaEntity.class));
 }
 @Test void visualWarningNeedsRealVisualChangeAndGeneratedImages(){
  CharacterImageConditionPort images=mock(CharacterImageConditionPort.class); when(images.hasGeneratedImages(7L)).thenReturn(true); Fixture fixture=new Fixture(Optional.empty(),Optional.of(images)); when(fixture.agent.review(any())).thenReturn(proposal("nuevo visual"));
  assertTrue(fixture.service.request(command(CharacterReviewTarget.VISUAL,false)).visualConsistencyWarning()); verify(images).hasGeneratedImages(7L);
 }
 @Test void narrativeRequestDoesNotConsultImageProvider(){
  CharacterImageConditionPort images=mock(CharacterImageConditionPort.class); Fixture fixture=new Fixture(Optional.empty(),Optional.of(images)); when(fixture.agent.review(any())).thenReturn(proposal("nuevo visual"));
  assertFalse(fixture.service.request(command(CharacterReviewTarget.NARRATIVE,false)).visualConsistencyWarning()); verifyNoInteractions(images);
 }
 private CatalogCharacterReviewCommand command(CharacterReviewTarget target,boolean replace){return new CatalogCharacterReviewCommand(7L,target,"haz el cambio",replace);}
 private CatalogCharacterReviewAgentResult proposal(String visual){return new CatalogCharacterReviewAgentResult(true,null,"Luna","nueva narrativa",visual,"new visual",List.of("orange"));}
 private static final class Fixture {
  final CharacterAgent agent=mock(CharacterAgent.class); final CatalogCharacterJpaRepository characters=mock(CatalogCharacterJpaRepository.class); final CatalogCharacterReviewJpaRepository reviews=mock(CatalogCharacterReviewJpaRepository.class); final Optional<CharacterImageConditionPort> images; final CharacterReviewService service;
  Fixture(Optional<CatalogCharacterReviewJpaEntity> pending,Optional<CharacterImageConditionPort> images){this.images=images; CatalogCharacterJpaEntity character=new CatalogCharacterJpaEntity();character.setName("Luna");character.setNarrativeDescription("narrativa");character.setVisualDescription("visual actual");character.setMetadata(new ActorMetadata("visual",List.of()));when(characters.findByIdForUpdate(7L)).thenReturn(Optional.of(character));when(reviews.findByCatalogCharacterIdAndStatus(7L,RevisionStatus.PENDING)).thenReturn(pending);service=new CharacterReviewService(agent,characters,reviews,images);}
 }
}
