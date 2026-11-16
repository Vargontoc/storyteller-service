package es.vargontoc.storyteller.domain.events;

import es.vargontoc.storyteller.domain.enums.PortraitGenerationEventType;
import es.vargontoc.storyteller.domain.enums.PortraitGenerationStatus;
import java.util.Objects;

public record PortraitGenerationEvent(
    Long catalogCharacterId,
    PortraitGenerationEventType type,
    PortraitGenerationStatus status,
    String reason
) {
    public PortraitGenerationEvent {
        Objects.requireNonNull(catalogCharacterId, "catalogCharacterId es obligatorio");
        Objects.requireNonNull(type, "type es obligatorio");
        Objects.requireNonNull(status, "status es obligatorio");
        Objects.requireNonNull(reason, "reason es obligatorio");
    }

    public static PortraitGenerationEvent started(Long catalogCharacterId) {
        return new PortraitGenerationEvent(catalogCharacterId, PortraitGenerationEventType.STARTED,
            PortraitGenerationStatus.RUNNING, "La generación de retratos ha comenzado.");
    }

    public static PortraitGenerationEvent progress(Long catalogCharacterId) {
        return new PortraitGenerationEvent(catalogCharacterId, PortraitGenerationEventType.PROGRESS,
            PortraitGenerationStatus.RUNNING, "El progreso de retratos está disponible.");
    }

    public static PortraitGenerationEvent success(Long catalogCharacterId) {
        return new PortraitGenerationEvent(catalogCharacterId, PortraitGenerationEventType.SUCCESS,
            PortraitGenerationStatus.COMPLETED, "Los retratos se generaron correctamente.");
    }

    public static PortraitGenerationEvent error(Long catalogCharacterId, String publicReason) {
        String reason = publicReason == null || publicReason.isBlank()
            ? "No se pudieron generar los retratos. Inténtalo de nuevo."
            : publicReason;
        return new PortraitGenerationEvent(catalogCharacterId, PortraitGenerationEventType.ERROR,
            PortraitGenerationStatus.FAILED, reason);
    }
}
