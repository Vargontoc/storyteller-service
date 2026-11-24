package es.vargontoc.storyteller.domain.response;

import java.util.List;

import es.vargontoc.storyteller.domain.model.CoverPlan;

/**
 * Resultado de la revisión global. Si el hint se acepta, trae el guion completo revisado;
 * si se rechaza, solo el motivo y el resto de campos quedan a null.
 */
public record DirectorReviewResult(
        boolean hintAccepted,
        String rejectionReason,
        String title,
        String synopsis,
        List<DirectorCastMember> cast,
        List<DirectorBeat> beats,
        CoverPlan cover) {

    /** Resultado sin planteamiento de portada. */
    public DirectorReviewResult(boolean hintAccepted, String rejectionReason, String title, String synopsis,
            List<DirectorCastMember> cast, List<DirectorBeat> beats) {
        this(hintAccepted, rejectionReason, title, synopsis, cast, beats, null);
    }
}
