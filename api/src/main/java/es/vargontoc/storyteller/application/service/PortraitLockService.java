package es.vargontoc.storyteller.application.service;

import es.vargontoc.storyteller.application.ports.out.persistence.PortraitGenerationJobStore;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortraitLockService {
    private final PortraitGenerationJobStore jobs;

    public PortraitLockService(PortraitGenerationJobStore jobs) {
        this.jobs = jobs;
    }

    @Transactional(readOnly = true)
    public boolean isLocked(Long catalogCharacterId) {
        return catalogCharacterId != null && jobs.findRunningByCatalogCharacterId(catalogCharacterId).isPresent();
    }

    public void requireUnlocked(Long catalogCharacterId) {
        if (isLocked(catalogCharacterId)) {
            throw new AppException("El personaje tiene una generación de retratos en curso", HttpStatus.CONFLICT);
        }
    }

    public void assertCanUseAsStoryProtagonist(Long catalogCharacterId) {
        requireUnlocked(catalogCharacterId);
    }
}
