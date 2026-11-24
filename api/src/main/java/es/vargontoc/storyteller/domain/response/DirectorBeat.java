package es.vargontoc.storyteller.domain.response;

import java.util.List;

/** Beat de la escaleta tal como lo devuelve el director: un beat por página interior. */
public record DirectorBeat(
        String event,
        String narrativeFunction,
        List<String> participants,
        String tone,
        String visualNote,
        String setting) {

    /** Beat sin lugar de escena (el lugar queda ausente: no se inventa). */
    public DirectorBeat(String event, String narrativeFunction, List<String> participants, String tone, String visualNote) {
        this(event, narrativeFunction, participants, tone, visualNote, null);
    }
}
