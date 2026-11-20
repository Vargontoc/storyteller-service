package es.vargontoc.storyteller.domain.response;

import java.util.List;

/** Beat de la escaleta tal como lo devuelve el director: un beat por página interior. */
public record DirectorBeat(
        String event,
        String narrativeFunction,
        List<String> participants,
        String tone,
        String visualNote) { }
