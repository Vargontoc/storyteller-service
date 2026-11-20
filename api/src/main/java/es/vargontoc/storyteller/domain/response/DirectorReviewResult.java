package es.vargontoc.storyteller.domain.response;

import java.util.List;

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
        List<DirectorBeat> beats) { }
