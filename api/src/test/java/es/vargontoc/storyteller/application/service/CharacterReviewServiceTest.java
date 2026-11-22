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
 @Test void confirmsOnlyNarrativeAndResolvesReview(){
  Fixture fixture=new Fixture(Optional.empty(),Optional.empty()); CatalogCharacterReviewJpaEntity review=pending(CharacterReviewTarget.NARRATIVE); when(fixture.reviews.findByIdAndStatus(9L,RevisionStatus.PENDING)).thenReturn(Optional.of(review));
  fixture.service.resolve(new es.vargontoc.storyteller.domain.command.CatalogCharacterReviewResolutionCommand(7L,9L,RevisionStatus.CONFIRMED));
  assertEquals(RevisionStatus.CONFIRMED,review.getStatus()); verify(fixture.characters).save(argThat(character->"nueva narrativa".equals(character.getNarrativeDescription())&&"visual actual".equals(character.getVisualDescription())));
 }
 @Test void discardPreservesCatalogCharacter(){
  Fixture fixture=new Fixture(Optional.empty(),Optional.empty()); CatalogCharacterReviewJpaEntity review=pending(CharacterReviewTarget.BOTH); when(fixture.reviews.findByIdAndStatus(9L,RevisionStatus.PENDING)).thenReturn(Optional.of(review));
  fixture.service.resolve(new es.vargontoc.storyteller.domain.command.CatalogCharacterReviewResolutionCommand(7L,9L,RevisionStatus.DISCARDED));
  assertEquals(RevisionStatus.DISCARDED,review.getStatus()); verify(fixture.characters,never()).save(any());
 }
 @Test void confirmsVisualAndBothOnlyApplyTheirAllowedFields(){
  Fixture visualFixture=new Fixture(Optional.empty(),Optional.empty()); CatalogCharacterReviewJpaEntity visual=pending(CharacterReviewTarget.VISUAL); when(visualFixture.reviews.findByIdAndStatus(9L,RevisionStatus.PENDING)).thenReturn(Optional.of(visual)); visualFixture.service.resolve(new es.vargontoc.storyteller.domain.command.CatalogCharacterReviewResolutionCommand(7L,9L,RevisionStatus.CONFIRMED)); verify(visualFixture.characters).save(argThat(character->"Luna".equals(character.getName())&&"narrativa".equals(character.getNarrativeDescription())&&"nuevo visual".equals(character.getVisualDescription())));
  Fixture bothFixture=new Fixture(Optional.empty(),Optional.empty()); CatalogCharacterReviewJpaEntity both=pending(CharacterReviewTarget.BOTH); when(bothFixture.reviews.findByIdAndStatus(9L,RevisionStatus.PENDING)).thenReturn(Optional.of(both)); bothFixture.service.resolve(new es.vargontoc.storyteller.domain.command.CatalogCharacterReviewResolutionCommand(7L,9L,RevisionStatus.CONFIRMED)); verify(bothFixture.characters).save(argThat(character->"Luna".equals(character.getName())&&"nueva narrativa".equals(character.getNarrativeDescription())&&"nuevo visual".equals(character.getVisualDescription())));
 }
 @Test void cannotResolveReviewThatIsNotPending(){Fixture fixture=new Fixture(Optional.empty(),Optional.empty());when(fixture.reviews.findByIdAndStatus(9L,RevisionStatus.PENDING)).thenReturn(Optional.empty());assertThrows(es.vargontoc.storyteller.shared.exceptions.AppException.class,()->fixture.service.resolve(new es.vargontoc.storyteller.domain.command.CatalogCharacterReviewResolutionCommand(7L,9L,RevisionStatus.CONFIRMED)));verify(fixture.characters,never()).save(any());}
 @Test void cannotResolveReviewFromAnotherCatalogCharacter(){Fixture fixture=new Fixture(Optional.empty(),Optional.empty());CatalogCharacterReviewJpaEntity review=pending(CharacterReviewTarget.NARRATIVE);CatalogCharacterJpaEntity other=mock(CatalogCharacterJpaEntity.class);when(other.getId()).thenReturn(8L);review.setCatalogCharacter(other);when(fixture.reviews.findByIdAndStatus(9L,RevisionStatus.PENDING)).thenReturn(Optional.of(review));assertThrows(es.vargontoc.storyteller.shared.exceptions.AppException.class,()->fixture.service.resolve(new es.vargontoc.storyteller.domain.command.CatalogCharacterReviewResolutionCommand(7L,9L,RevisionStatus.CONFIRMED)));verify(fixture.characters,never()).save(any());verify(fixture.reviews,never()).save(any());}
 @Test void lockRejectsRequestAndResolutionBeforeAgentOrWrites(){
  Fixture requestFixture=new Fixture(Optional.empty(),Optional.empty());doThrow(new es.vargontoc.storyteller.shared.exceptions.AppException("bloqueado",org.springframework.http.HttpStatus.CONFLICT)).when(requestFixture.lock).requireUnlocked(7L);
  assertThrows(es.vargontoc.storyteller.shared.exceptions.AppException.class,()->requestFixture.service.request(command(CharacterReviewTarget.VISUAL,false)));verifyNoInteractions(requestFixture.agent,requestFixture.reviews);
  Fixture resolveFixture=new Fixture(Optional.empty(),Optional.empty());doThrow(new es.vargontoc.storyteller.shared.exceptions.AppException("bloqueado",org.springframework.http.HttpStatus.CONFLICT)).when(resolveFixture.lock).requireUnlocked(7L);
  assertThrows(es.vargontoc.storyteller.shared.exceptions.AppException.class,()->resolveFixture.service.resolve(new es.vargontoc.storyteller.domain.command.CatalogCharacterReviewResolutionCommand(7L,9L,RevisionStatus.CONFIRMED)));verifyNoInteractions(resolveFixture.reviews);
 }
 @Test void visualChangeWithoutStoryUsageInvalidatesPortraitAndCandidates(){
  PortraitForkService forks=mock(PortraitForkService.class); es.vargontoc.storyteller.application.ports.out.persistence.CatalogCharacterUsagePort usage=mock(es.vargontoc.storyteller.application.ports.out.persistence.CatalogCharacterUsagePort.class); PortraitInvalidationService invalidation=mock(PortraitInvalidationService.class);
  Fixture fixture=new Fixture(Optional.empty(),Optional.empty(),forks,usage,invalidation); CatalogCharacterReviewJpaEntity review=pending(CharacterReviewTarget.VISUAL);when(fixture.reviews.findByIdAndStatus(9L,RevisionStatus.PENDING)).thenReturn(Optional.of(review));when(usage.isUsedByStorySnapshot(7L)).thenReturn(false);
  fixture.service.resolve(new es.vargontoc.storyteller.domain.command.CatalogCharacterReviewResolutionCommand(7L,9L,RevisionStatus.CONFIRMED));
  verify(invalidation).invalidate(7L); verify(fixture.characters).save(any()); verifyNoInteractions(forks);
 }
 @Test void usedVisualChangeRequiresConfirmationThenCreatesForkWithoutMutatingOriginal(){
  PortraitForkService forks=mock(PortraitForkService.class); es.vargontoc.storyteller.application.ports.out.persistence.CatalogCharacterUsagePort usage=mock(es.vargontoc.storyteller.application.ports.out.persistence.CatalogCharacterUsagePort.class);
  Fixture fixture=new Fixture(Optional.empty(),Optional.empty(),forks,usage,mock(PortraitInvalidationService.class)); CatalogCharacterReviewJpaEntity review=pending(CharacterReviewTarget.VISUAL);when(fixture.reviews.findByIdAndStatus(9L,RevisionStatus.PENDING)).thenReturn(Optional.of(review));when(usage.isUsedByStorySnapshot(7L)).thenReturn(true);
  when(forks.create(eq(7L),eq("nuevo visual"),any())).thenReturn(new es.vargontoc.storyteller.domain.model.CatalogCharacter(12L,"Luna (variante)","narrativa","nuevo visual","new",List.of()));
  var warning=fixture.service.resolve(new es.vargontoc.storyteller.domain.command.CatalogCharacterReviewResolutionCommand(7L,9L,RevisionStatus.CONFIRMED));
  assertTrue(warning.forkConfirmationRequired()); verifyNoInteractions(forks); verify(fixture.characters,never()).save(any());
  var confirmed=fixture.service.resolve(new es.vargontoc.storyteller.domain.command.CatalogCharacterReviewResolutionCommand(7L,9L,RevisionStatus.CONFIRMED,true));
  assertFalse(confirmed.forkConfirmationRequired()); verify(forks).create(eq(7L),eq("nuevo visual"),any()); verify(fixture.characters,never()).save(any());
 }
 private static final es.vargontoc.storyteller.application.ports.out.GenerationGatePort.GenerationType WRITER=es.vargontoc.storyteller.application.ports.out.GenerationGatePort.GenerationType.WRITER;
 @Test void busyGateRejectsWithConflictWithoutAgentNorDiscardingPending(){
  CatalogCharacterReviewJpaEntity previous=new CatalogCharacterReviewJpaEntity(); previous.setStatus(RevisionStatus.PENDING); Fixture fixture=new Fixture(Optional.of(previous),Optional.empty()); assertTrue(fixture.gate.tryAcquire(WRITER,5L));
  var e=assertThrows(es.vargontoc.storyteller.shared.exceptions.AppException.class,()->fixture.service.request(command(CharacterReviewTarget.NARRATIVE,true)));
  assertEquals(org.springframework.http.HttpStatus.CONFLICT,e.getStatus()); assertEquals("Hay una generación en curso",e.getMessage()); assertEquals(RevisionStatus.PENDING,previous.getStatus()); verifyNoInteractions(fixture.agent); verify(fixture.reviews,never()).saveAndFlush(any()); verify(fixture.reviews,never()).save(any(CatalogCharacterReviewJpaEntity.class)); assertFalse(fixture.gate.tryAcquire(WRITER,null));
 }
 @Test void requestReleasesGateOnSuccessAndOnAgentFailure(){
  Fixture fixture=new Fixture(Optional.empty(),Optional.empty()); when(fixture.agent.review(any())).thenReturn(proposal("nuevo visual")); fixture.service.request(command(CharacterReviewTarget.NARRATIVE,false)); assertTrue(fixture.gate.tryAcquire(WRITER,null)); fixture.gate.release();
  when(fixture.agent.review(any())).thenThrow(new IllegalStateException("ollama")); assertThrows(IllegalStateException.class,()->fixture.service.request(command(CharacterReviewTarget.NARRATIVE,false))); assertTrue(fixture.gate.tryAcquire(WRITER,null));
 }
 private CatalogCharacterReviewJpaEntity pending(CharacterReviewTarget target){CatalogCharacterReviewJpaEntity review=new CatalogCharacterReviewJpaEntity();review.setTarget(target);review.setStatus(RevisionStatus.PENDING);review.setHint("cambio");review.setCandidateName("Nueva Luna");review.setCandidateNarrative("nueva narrativa");review.setCandidateVisual("nuevo visual");review.setMetadata(new ActorMetadata("new",List.of("new")));CatalogCharacterJpaEntity character=mock(CatalogCharacterJpaEntity.class);when(character.getId()).thenReturn(7L);review.setCatalogCharacter(character);return review;}
 private CatalogCharacterReviewCommand command(CharacterReviewTarget target,boolean replace){return new CatalogCharacterReviewCommand(7L,target,"haz el cambio",replace);}
 private CatalogCharacterReviewAgentResult proposal(String visual){return new CatalogCharacterReviewAgentResult(true,null,"Luna","nueva narrativa",visual,"new visual",List.of("orange"));}
 private static final class Fixture {
  final CharacterAgent agent=mock(CharacterAgent.class); final CatalogCharacterJpaRepository characters=mock(CatalogCharacterJpaRepository.class); final CatalogCharacterReviewJpaRepository reviews=mock(CatalogCharacterReviewJpaRepository.class); final Optional<CharacterImageConditionPort> images; final PortraitLockService lock=mock(PortraitLockService.class); final GenerationGate gate=new GenerationGate(); final CharacterReviewService service;
  Fixture(Optional<CatalogCharacterReviewJpaEntity> pending,Optional<CharacterImageConditionPort> images){this(pending,images,mock(PortraitForkService.class),mock(es.vargontoc.storyteller.application.ports.out.persistence.CatalogCharacterUsagePort.class),mock(PortraitInvalidationService.class));}
  Fixture(Optional<CatalogCharacterReviewJpaEntity> pending,Optional<CharacterImageConditionPort> images,PortraitForkService forks,es.vargontoc.storyteller.application.ports.out.persistence.CatalogCharacterUsagePort usage,PortraitInvalidationService invalidation){this.images=images; CatalogCharacterJpaEntity character=new CatalogCharacterJpaEntity();character.setName("Luna");character.setNarrativeDescription("narrativa");character.setVisualDescription("visual actual");character.setMetadata(new ActorMetadata("visual",List.of()));when(characters.findByIdForUpdate(7L)).thenReturn(Optional.of(character));when(reviews.findByCatalogCharacterIdAndStatus(7L,RevisionStatus.PENDING)).thenReturn(pending);service=new CharacterReviewService(agent,characters,reviews,images,lock,gate,Optional.of(forks),Optional.of(usage),Optional.of(invalidation));}
 }
}
