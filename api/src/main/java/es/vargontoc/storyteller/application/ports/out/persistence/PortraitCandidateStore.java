package es.vargontoc.storyteller.application.ports.out.persistence;

import es.vargontoc.storyteller.domain.model.PortraitCandidate;
import java.util.List;

public interface PortraitCandidateStore {
    List<PortraitCandidate> replace(Long catalogCharacterId, Long generationJobId, List<String> assetPaths);
    List<PortraitCandidate> findByCatalogCharacterId(Long catalogCharacterId);
    void delete(Long catalogCharacterId, Long candidateId);
    void deleteAll(Long catalogCharacterId);
    void deleteByGenerationJobId(Long generationJobId);
}
