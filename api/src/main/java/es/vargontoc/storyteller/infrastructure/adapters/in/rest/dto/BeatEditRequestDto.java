package es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto;

import java.util.List;

import es.vargontoc.storyteller.domain.model.StoryBeat;

/**
 * Cuerpo de la edición de un beat. {@code confirmInvalidation}: ausente en la primera petición;
 * true para confirmar el aviso de invalidación de páginas y false para cancelarlo.
 */
public record BeatEditRequestDto(
    String event,
    String narrativeFunction,
    List<String> participants,
    String tone,
    String visualNote,
    String setting,
    Boolean confirmInvalidation) {

    public StoryBeat toBeat() {
        return new StoryBeat(event, narrativeFunction, participants, tone, visualNote, setting);
    }
}
