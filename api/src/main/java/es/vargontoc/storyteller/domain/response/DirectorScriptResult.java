package es.vargontoc.storyteller.domain.response;

import java.util.List;

import es.vargontoc.storyteller.domain.model.CoverPlan;

/**
 * Guion completo devuelto por el director al generar o al revisar con un hint aceptado.
 * {@code cover} es el planteamiento de portada (lugar y tono); queda a null si el director no lo devuelve.
 */
public record DirectorScriptResult(
        String title,
        String synopsis,
        List<DirectorCastMember> cast,
        List<DirectorBeat> beats,
        CoverPlan cover) {

    /** Guion sin planteamiento de portada. */
    public DirectorScriptResult(String title, String synopsis, List<DirectorCastMember> cast, List<DirectorBeat> beats) {
        this(title, synopsis, cast, beats, null);
    }
}
