package es.vargontoc.storyteller.domain.model;

import java.util.List;

/**
 * Escaleta de un cuento: reparto episodico ligero y un beat por pagina interior (el beat i es la pagina i + 1).
 * Titulo y sinopsis viven en {@link Story}.
 */
public record StoryOutline(List<EpisodicCastMember> cast, List<StoryBeat> beats) {

    /** Valida antes de persistir: al menos un beat, todos completos, y reparto completo. */
    public boolean isComplete() {
        return cast != null && cast.stream().allMatch(EpisodicCastMember::isComplete)
                && beats != null && !beats.isEmpty() && beats.stream().allMatch(StoryBeat::isComplete);
    }
}
