package es.vargontoc.storyteller.domain.response;

import es.vargontoc.storyteller.domain.enums.BeatEditStatus;
import es.vargontoc.storyteller.domain.model.Story;

/**
 * Resultado de la edición de un beat. {@code reason} solo con REJECTED; {@code invalidatedPages} con
 * CONFIRMATION_REQUIRED (páginas que se eliminarían) y con APPLIED (páginas eliminadas); {@code story} solo con APPLIED.
 */
public record BeatEditResult(BeatEditStatus status, String reason, int invalidatedPages, Story story) {

    public static BeatEditResult applied(Story story, int removedPages) {
        return new BeatEditResult(BeatEditStatus.APPLIED, null, removedPages, story);
    }

    public static BeatEditResult rejected(String reason) {
        return new BeatEditResult(BeatEditStatus.REJECTED, reason, 0, null);
    }

    public static BeatEditResult confirmationRequired(int pages) {
        return new BeatEditResult(BeatEditStatus.CONFIRMATION_REQUIRED, null, pages, null);
    }

    public static BeatEditResult cancelled() {
        return new BeatEditResult(BeatEditStatus.CANCELLED, null, 0, null);
    }
}
