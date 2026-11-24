package es.vargontoc.storyteller.domain.model;

import java.util.List;

/**
 * Beat de la escaleta: lo que ocurre en una pagina interior. Tiene cinco campos obligatorios y el lugar de escena (`setting`, nulo en escaletas antiguas).
 * Se puede construir incompleto (por ejemplo, desde la respuesta del director) pero {@link #isComplete()}
 * permite detectarlo antes de persistir.
 */
public record StoryBeat(
        String event,
        String narrativeFunction,
        List<String> participants,
        String tone,
        String visualNote,
        String setting) {

    /** Beat sin lugar de escena (el lugar es nulo, como en las escaletas anteriores a la feature). */
    public StoryBeat(String event, String narrativeFunction, List<String> participants, String tone, String visualNote) {
        this(event, narrativeFunction, participants, tone, visualNote, null);
    }

    public boolean isComplete() {
        return notBlank(event)
                && notBlank(narrativeFunction)
                && participants != null && !participants.isEmpty() && participants.stream().allMatch(StoryBeat::notBlank)
                && notBlank(tone)
                && notBlank(visualNote);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
