package es.vargontoc.storyteller.domain.model;

import java.util.List;

/**
 * Escaleta de un cuento: reparto episodico ligero y un beat por pagina interior (el beat i es la pagina i + 1).
 * Titulo y sinopsis viven en {@link Story}. {@code cover} es el planteamiento de la portada, separado de los beats.
 */
public record StoryOutline(List<EpisodicCastMember> cast, List<StoryBeat> beats, CoverPlan cover) {

    /** Escaleta sin planteamiento de portada (nulo, como en las escaletas anteriores a la feature). */
    public StoryOutline(List<EpisodicCastMember> cast, List<StoryBeat> beats) {
        this(cast, beats, null);
    }

    /** Valida antes de persistir: al menos un beat, todos completos, y reparto completo. */
    public boolean isComplete() {
        return cast != null && cast.stream().allMatch(EpisodicCastMember::isComplete)
                && beats != null && !beats.isEmpty() && beats.stream().allMatch(StoryBeat::isComplete);
    }
}
