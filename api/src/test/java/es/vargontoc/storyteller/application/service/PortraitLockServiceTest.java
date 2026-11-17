package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import es.vargontoc.storyteller.application.ports.out.persistence.PortraitGenerationJobStore;
import es.vargontoc.storyteller.domain.enums.PortraitGenerationStatus;
import es.vargontoc.storyteller.domain.model.PortraitGenerationJob;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PortraitLockServiceTest {
    private final PortraitGenerationJobStore jobs = Mockito.mock(PortraitGenerationJobStore.class);
    private final PortraitLockService service = new PortraitLockService(jobs);

    @Test
    void runningJobLocksWithoutWritingOrExpiring() {
        when(jobs.findRunningByCatalogCharacterId(7L)).thenReturn(Optional.of(job(PortraitGenerationStatus.RUNNING)));

        assertTrue(service.isLocked(7L));
        assertTrue(service.isLocked(7L));
        assertThrows(AppException.class, () -> service.requireUnlocked(7L));
        assertThrows(AppException.class, () -> service.assertCanUseAsStoryProtagonist(7L));

        verify(jobs, Mockito.times(4)).findRunningByCatalogCharacterId(7L);
        verifyNoMoreInteractions(jobs);
    }

    @Test
    void completedOrFailedJobsReleaseTheLock() {
        when(jobs.findRunningByCatalogCharacterId(8L)).thenReturn(Optional.empty());

        assertFalse(service.isLocked(8L));
        service.requireUnlocked(8L);
        assertFalse(service.isLocked(null));
    }

    private PortraitGenerationJob job(PortraitGenerationStatus status) {
        return new PortraitGenerationJob(1L, 7L, status, null);
    }
}
