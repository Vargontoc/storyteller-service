package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.application.ports.out.external.WriterAgent;
import es.vargontoc.storyteller.domain.model.EpisodicCastMember;
import es.vargontoc.storyteller.domain.model.SceneComposition;
import es.vargontoc.storyteller.domain.model.StoryBeat;
import es.vargontoc.storyteller.domain.request.WriterContext;
import es.vargontoc.storyteller.domain.response.WriterHintResult;
import es.vargontoc.storyteller.domain.response.WriterResult;
import es.vargontoc.storyteller.shared.Constants;
import es.vargontoc.storyteller.shared.exceptions.AppException;

/**
 * Adaptador del escritor sobre Ollama. Las reglas de escritura viven en {@code prompts/writer.modelfile};
 * aquí solo se arma el mensaje de cada modo y se valida el resultado antes de entregarlo al servicio.
 * Cualquier respuesta incompleta o error del modelo se devuelve como {@link AppException}.
 */
@Component
public class SpringAiWriterAgent implements WriterAgent {

    static final String PAGE_ONOMATOPOEIA_RULES = """
            ONOMATOPEYAS DE ESTA PÁGINA:
            - Como máximo una onomatopeya.
            - Solo si el beat de esta página tiene un sonido natural en la escena; si el beat es silencioso, no incluyas ninguna.
            - Usa la forma estándar y pronunciable del español («guau», «pum»), sin letras repetidas alargadas ni símbolos.
            """;
    static final String COVER_ONOMATOPOEIA_RULE = "ONOMATOPEYAS: no incluyas ninguna onomatopeya.\n";

    private final OllamaPort ollama;
    private final String model;
    private final ChatClient client;

    public SpringAiWriterAgent(
            OllamaPort ollama,
            @Qualifier(Constants.BeanNames.AGENT_SCRIPTWRITER_MODEL) String model,
            @Qualifier(Constants.BeanNames.AGENT_SCRIPTWRITER) ChatClient client) {
        this.ollama = ollama;
        this.model = model;
        this.client = client;
    }

    @Override
    public WriterResult writeCover(WriterContext context) {
        String message = "MODO: ESCENA_PORTADA\n" + describeBook(context) + COVER_ONOMATOPOEIA_RULE;
        WriterResult result = call(message, WriterResult.class);
        if (!hasScene(result == null ? null : result.scene())) {
            throw invalidResponse();
        }
        return new WriterResult(null, result.scene());
    }

    @Override
    public WriterResult writePage(WriterContext context) {
        String message = "MODO: ESCRIBIR_PAGINA\n" + describeBook(context) + PAGE_ONOMATOPOEIA_RULES;
        WriterResult result = call(message, WriterResult.class);
        if (result == null || isBlank(result.text()) || !hasScene(result.scene())) {
            throw invalidResponse();
        }
        return new WriterResult(result.text().trim(), result.scene());
    }

    @Override
    public WriterHintResult rewrite(WriterContext context, WriterResult base, String hint) {
        String message = "MODO: NUEVA_VERSION\n" + describeBook(context)
                + "Versión base:\n  texto: " + (base.text() == null ? "(portada, sin texto)" : base.text())
                + "\n  escena: " + base.scene().scene()
                + "\nHint del adulto: " + hint + "\n"
                + (context.isCover() ? COVER_ONOMATOPOEIA_RULE : PAGE_ONOMATOPOEIA_RULES);
        WriterHintResult result = call(message, WriterHintResult.class);
        if (result == null) {
            throw invalidResponse();
        }
        if (!result.hintAccepted()) {
            if (isBlank(result.rejectionReason())) {
                throw invalidResponse();
            }
            return new WriterHintResult(false, result.rejectionReason().trim(), null, null);
        }
        if ((!context.isCover() && isBlank(result.text())) || !hasScene(result.scene())) {
            throw invalidResponse();
        }
        String text = context.isCover() ? null : result.text().trim();
        return new WriterHintResult(true, null, text, result.scene());
    }

    private <T> T call(String message, Class<T> type) {
        if (!ollama.isAvailable(model)) {
            throw new AppException("El agente escritor no está disponible", HttpStatus.BAD_REQUEST);
        }
        try {
            return client.prompt().user(message).call().entity(type);
        } catch (RuntimeException e) {
            throw new AppException("El agente escritor falló: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /** Contexto del cuento; beat, reparto y páginas anteriores solo cuando no es la portada. */
    private String describeBook(WriterContext context) {
        var text = new StringBuilder();
        text.append("Título: ").append(context.title()).append('\n')
                .append("Sinopsis: ").append(context.synopsis()).append('\n')
                .append("Protagonista: ").append(context.protagonist()).append('\n');
        if (context.isCover()) {
            return text.toString();
        }
        text.append("Reparto episódico: ").append(describeCast(context.cast())).append('\n')
                .append("Beat de la página:\n").append(describeBeat(context.beat())).append('\n')
                .append("Texto de las páginas anteriores:\n").append(describePrevious(context.previousTexts()));
        return text.toString();
    }

    private String describeBeat(StoryBeat beat) {
        return "  qué ocurre: " + beat.event()
                + "\n  función narrativa: " + beat.narrativeFunction()
                + "\n  quiénes aparecen: " + String.join(", ", beat.participants())
                + "\n  tono: " + beat.tone()
                + "\n  nota visual: " + beat.visualNote();
    }

    private String describeCast(List<EpisodicCastMember> cast) {
        if (cast == null || cast.isEmpty()) {
            return "(sin secundarios)";
        }
        return cast.stream().map(member -> member.name() + ": " + member.role()).collect(Collectors.joining("; "));
    }

    private String describePrevious(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return "  (ninguna)\n";
        }
        var text = new StringBuilder();
        for (int i = 0; i < texts.size(); i++) {
            text.append("  Página ").append(i + 1).append(": ").append(texts.get(i)).append('\n');
        }
        return text.toString();
    }

    private boolean hasScene(SceneComposition scene) {
        return scene != null && !isBlank(scene.scene());
    }

    private AppException invalidResponse() {
        return new AppException("El agente escritor devolvió una respuesta incompleta", HttpStatus.BAD_REQUEST);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
