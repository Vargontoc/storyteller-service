package es.vargontoc.storyteller.application.service;

import static org.mockito.Mockito.*;
import es.vargontoc.storyteller.application.ports.out.PortraitGenerationTaskRegistry;
import es.vargontoc.storyteller.application.ports.out.persistence.*;
import es.vargontoc.storyteller.domain.enums.PortraitGenerationStatus;
import es.vargontoc.storyteller.domain.model.PortraitGenerationJob;
import java.util.List;
import org.junit.jupiter.api.Test;

class PortraitGenerationReconciliationServiceTest {
 @Test void failsOnlyOrphanRunningJobsAndKeepsLiveOnes(){
  PortraitGenerationJobStore jobs=mock(PortraitGenerationJobStore.class); PortraitCandidateStore candidates=mock(PortraitCandidateStore.class); PortraitWebsocketNotifier notifier=mock(PortraitWebsocketNotifier.class); PortraitGenerationTaskRegistry tasks=mock(PortraitGenerationTaskRegistry.class);
  var orphan=new PortraitGenerationJob(1L,7L,PortraitGenerationStatus.RUNNING,null); var live=new PortraitGenerationJob(2L,8L,PortraitGenerationStatus.RUNNING,null);
  when(jobs.findRunning()).thenReturn(List.of(orphan,live)); when(tasks.isAlive(1L)).thenReturn(false); when(tasks.isAlive(2L)).thenReturn(true);
  new PortraitGenerationReconciliationService(jobs,candidates,notifier,tasks).reconcile();
  verify(jobs).fail(eq(1L),anyString()); verify(candidates).deleteByGenerationJobId(1L); verify(notifier).error(eq(7L),anyString()); verify(jobs,never()).fail(eq(2L),anyString()); verify(candidates,never()).deleteByGenerationJobId(2L);
 }
}
