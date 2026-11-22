package es.vargontoc.storyteller.domain.response;

import es.vargontoc.storyteller.domain.enums.BeatEditStatus;
import es.vargontoc.storyteller.domain.model.Story;

/**
 * Resultado de la edición de un beat. {@code reason} solo con REJECTED; {@code invalidatedPages} con
 * CONFIRMATION_REQUIRED (páginas que se eliminarían) y con APPLIED (páginas eliminadas); {@code story} solo con APPLIED.
 * {@code audioWillBeDeleted} es true solo en CONFIRMATION_REQUIRED de un cuento con audio narrado, que se borrará con las páginas.
 */
public record BeatEditResult(BeatEditStatus status, String reason, int invalidatedPages, Story story,
        boolean audioWillBeDeleted) {

    public static BeatEditResult applied(Story story, int removedPages) {
        return new BeatEditResult(BeatEditStatus.APPLIED, null, removedPages, story, false);
    }

    public static BeatEditResult rejected(String reason) {
        return new BeatEditResult(BeatEditStatus.REJECTED, reason, 0, null, false);
    }

    public static BeatEditResult confirmationRequired(int pages) {
        return confirmationRequired(pages, false);
    }

    public static BeatEditResult confirmationRequired(int pages, boolean audioWillBeDeleted) {
        return new BeatEditResult(BeatEditStatus.CONFIRMATION_REQUIRED, null, pages, null, audioWillBeDeleted);
    }

    public static BeatEditResult cancelled() {
        return new BeatEditResult(BeatEditStatus.CANCELLED, null, 0, null, false);
    }
}
