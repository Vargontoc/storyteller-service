package es.vargontoc.storyteller.application.ports.out.external;

import es.vargontoc.storyteller.domain.request.DirectorReviewRequest;
import es.vargontoc.storyteller.domain.request.DirectorScriptRequest;
import es.vargontoc.storyteller.domain.response.DirectorBeat;
import es.vargontoc.storyteller.domain.response.DirectorBeatVerdict;
import es.vargontoc.storyteller.domain.response.DirectorReviewResult;
import es.vargontoc.storyteller.domain.response.DirectorScriptResult;

/** Puerto del agente director: guion con escaleta, revisión global y validación de edición de beat. */
public interface DirectorAgent {

    DirectorScriptResult generateScript(DirectorScriptRequest request);

    DirectorReviewResult reviewScript(DirectorReviewRequest request);

    DirectorBeatVerdict validateBeat(DirectorBeat editedBeat);
}
