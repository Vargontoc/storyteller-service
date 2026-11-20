package es.vargontoc.storyteller.domain.response;

import java.util.List;

import es.vargontoc.storyteller.domain.enums.StoryStatus;
import es.vargontoc.storyteller.domain.model.EpisodicCastMember;

/**
 * Guion vigente de un cuento: titulo, sinopsis, estado, version, reparto ligero y escaleta.
 * En un cuento antiguo {@code scriptVersion} es nulo y {@code cast} y {@code beats} van vacios.
 */
public record StoryScript(
        Long id,
        String title,
        String synopsis,
        StoryStatus status,
        Integer scriptVersion,
        List<EpisodicCastMember> cast,
        List<ScriptBeat> beats) {

    /** Beat de la escaleta con su numero de pagina explicito (desde 1). */
    public record ScriptBeat(
            int page,
            String event,
            String narrativeFunction,
            List<String> participants,
            String tone,
            String visualNote) {
    }
}
