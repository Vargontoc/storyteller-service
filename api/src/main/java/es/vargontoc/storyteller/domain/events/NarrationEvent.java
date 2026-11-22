package es.vargontoc.storyteller.domain.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import es.vargontoc.storyteller.domain.enums.NarrationEventType;
import java.util.Objects;

/** Evento de narracion de un cuento. voiceName solo se informa en COMPLETED. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NarrationEvent(
    NarrationEventType type,
    Long storyId,
    String voiceName
) {
    public NarrationEvent {
        Objects.requireNonNull(type, "type es obligatorio");
        Objects.requireNonNull(storyId, "storyId es obligatorio");
    }
}
