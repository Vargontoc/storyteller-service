package es.vargontoc.storyteller.domain.events;

import es.vargontoc.storyteller.domain.enums.StoryPageEventType;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;

/** Evento de generación de la portada (pageNumber 0) o de una página. reason solo se informa en el rechazo del hint. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StoryPageGenerationEvent(
    StoryPageEventType type,
    Long storyId,
    int pageNumber,
    String reason
) {
    public StoryPageGenerationEvent {
        Objects.requireNonNull(type, "type es obligatorio");
        Objects.requireNonNull(storyId, "storyId es obligatorio");
    }
}
