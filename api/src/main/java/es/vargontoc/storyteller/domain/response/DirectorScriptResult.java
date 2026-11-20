package es.vargontoc.storyteller.domain.response;

import java.util.List;

/** Guion completo devuelto por el director al generar o al revisar con un hint aceptado. */
public record DirectorScriptResult(
        String title,
        String synopsis,
        List<DirectorCastMember> cast,
        List<DirectorBeat> beats) { }
