package es.vargontoc.storyteller.application.ports.out.persistence;

import es.vargontoc.storyteller.domain.model.PortraitGenerationJob;
import java.util.List;
import java.util.Optional;

public interface PortraitGenerationJobStore {
    PortraitGenerationJob start(Long catalogCharacterId);
    Optional<PortraitGenerationJob> findRunningByCatalogCharacterId(Long catalogCharacterId);
    PortraitGenerationJob complete(Long jobId);
    PortraitGenerationJob fail(Long jobId, String publicReason);
    List<PortraitGenerationJob> findRunning();
}
