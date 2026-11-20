package es.vargontoc.storyteller.domain.request;

import java.util.List;

import es.vargontoc.storyteller.domain.response.DirectorBeat;
import es.vargontoc.storyteller.domain.response.DirectorCastMember;

/** Entrada para la revisión global: guion vigente completo y hint del adulto. */
public record DirectorReviewRequest(
        String title,
        String synopsis,
        List<DirectorCastMember> cast,
        List<DirectorBeat> beats,
        String hint) { }
