package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.application.ports.out.external.DirectorAgent;
import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.domain.request.DirectorReviewRequest;
import es.vargontoc.storyteller.domain.request.DirectorScriptRequest;
import es.vargontoc.storyteller.domain.response.DirectorBeat;
import es.vargontoc.storyteller.domain.response.DirectorBeatVerdict;
import es.vargontoc.storyteller.domain.response.DirectorCastMember;
import es.vargontoc.storyteller.domain.response.DirectorReviewResult;
import es.vargontoc.storyteller.domain.response.DirectorScriptResult;
import es.vargontoc.storyteller.shared.Constants;
import es.vargontoc.storyteller.shared.exceptions.AppException;

/**
 * Adaptador del director sobre Ollama. Las instrucciones viven en
 * {@code prompts/director.modelfile}; aquí solo se arma el mensaje de cada modo y se
 * valida el resultado tipado antes de entregarlo al servicio.
 */
@Component
public class SpringAiDirectorAgent implements DirectorAgent {

    private final OllamaPort ollama;
    private final String model;
    private final ChatClient client;

    public SpringAiDirectorAgent(
            OllamaPort ollama,
            @Qualifier(Constants.BeanNames.AGENT_DIRECTOR_MODEL) String model,
            @Qualifier(Constants.BeanNames.AGENT_DIRECTOR) ChatClient client) {
        this.ollama = ollama;
        this.model = model;
        this.client = client;
    }

    @Override
    public DirectorScriptResult generateScript(DirectorScriptRequest request) {
        if (request == null || request.pages() < 1) {
            throw invalidResponse();
        }
        ensureAvailable();
        String message = """
                MODO: GENERAR_GUION
                Tema: %s - %s
                Número de páginas interiores (beats): %d
                Protagonista:
                  nombre: %s
                  descripción narrativa: %s
                  descripción visual: %s
                  retrato aprobado: %s
                """.formatted(request.topic(), request.description(), request.pages(),
                request.protagonistName(), request.protagonistNarrative(), request.protagonistVisual(),
                request.protagonistCatalogId() == null ? "no indicado" : "catálogo #" + request.protagonistCatalogId());
        DirectorScriptResult result = client.prompt().user(message).call().entity(DirectorScriptResult.class);
        if (result == null) {
            throw invalidResponse();
        }
        return validateScript(result.title(), result.synopsis(), result.cast(), result.beats(), request.pages());
    }

    @Override
    public DirectorReviewResult reviewScript(DirectorReviewRequest request) {
        if (request == null || request.beats() == null || request.beats().isEmpty()) {
            throw invalidResponse();
        }
        ensureAvailable();
        String message = """
                MODO: REVISAR_GUION
                Guion vigente:
                  título: %s
                  sinopsis: %s
                  reparto: %s
                  escaleta (%d beats):
                %s
                Hint del adulto: %s
                """.formatted(request.title(), request.synopsis(), describeCast(request.cast()),
                request.beats().size(), describeBeats(request.beats()), request.hint());
        DirectorReviewResult result = client.prompt().user(message).call().entity(DirectorReviewResult.class);
        if (result == null) {
            throw invalidResponse();
        }
        if (!result.hintAccepted()) {
            if (isBlank(result.rejectionReason())) {
                throw invalidResponse();
            }
            return new DirectorReviewResult(false, result.rejectionReason().trim(), null, null, null, null);
        }
        DirectorScriptResult script = validateScript(
                result.title(), result.synopsis(), result.cast(), result.beats(), request.beats().size());
        return new DirectorReviewResult(true, null, script.title(), script.synopsis(), script.cast(), script.beats());
    }

    @Override
    public DirectorBeatVerdict validateBeat(DirectorBeat editedBeat) {
        if (!isComplete(editedBeat)) {
            throw invalidResponse();
        }
        ensureAvailable();
        String message = "MODO: VALIDAR_EDICION_BEAT\nBeat editado:\n" + describeBeat(editedBeat);
        DirectorBeatVerdict verdict = client.prompt().user(message).call().entity(DirectorBeatVerdict.class);
        if (verdict == null || (!verdict.accepted() && isBlank(verdict.reason()))) {
            throw invalidResponse();
        }
        return verdict.accepted() ? new DirectorBeatVerdict(true, null)
                : new DirectorBeatVerdict(false, verdict.reason().trim());
    }

    private DirectorScriptResult validateScript(String title, String synopsis, List<DirectorCastMember> cast,
            List<DirectorBeat> beats, int expectedBeats) {
        if (isBlank(title) || isBlank(synopsis) || beats == null || beats.size() != expectedBeats
                || !beats.stream().allMatch(this::isComplete)) {
            throw invalidResponse();
        }
        List<DirectorCastMember> safeCast = cast == null ? List.of() : cast;
        if (!safeCast.stream().allMatch(member -> member != null && !isBlank(member.name()) && !isBlank(member.role()))) {
            throw invalidResponse();
        }
        return new DirectorScriptResult(title.trim(), synopsis.trim(), safeCast, beats);
    }

    private boolean isComplete(DirectorBeat beat) {
        return beat != null
                && !isBlank(beat.event())
                && !isBlank(beat.narrativeFunction())
                && beat.participants() != null
                && !beat.participants().isEmpty()
                && beat.participants().stream().noneMatch(this::isBlank)
                && !isBlank(beat.tone())
                && !isBlank(beat.visualNote());
    }

    private String describeBeats(List<DirectorBeat> beats) {
        var text = new StringBuilder();
        for (int i = 0; i < beats.size(); i++) {
            text.append("    Página ").append(i + 1).append(": ").append(describeBeat(beats.get(i))).append('\n');
        }
        return text.toString().stripTrailing();
    }

    private String describeBeat(DirectorBeat beat) {
        return "event=" + beat.event()
                + " | narrativeFunction=" + beat.narrativeFunction()
                + " | participants=" + String.join(", ", beat.participants())
                + " | tone=" + beat.tone()
                + " | visualNote=" + beat.visualNote();
    }

    private String describeCast(List<DirectorCastMember> cast) {
        if (cast == null || cast.isEmpty()) {
            return "(sin secundarios)";
        }
        return cast.stream().map(member -> member.name() + ": " + member.role()).collect(Collectors.joining("; "));
    }

    private void ensureAvailable() {
        if (!ollama.isAvailable(model)) {
            throw new AppException("El agente director no está disponible", HttpStatus.BAD_REQUEST);
        }
    }

    private AppException invalidResponse() {
        return new AppException("El agente director devolvió una respuesta incompleta", HttpStatus.BAD_REQUEST);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
