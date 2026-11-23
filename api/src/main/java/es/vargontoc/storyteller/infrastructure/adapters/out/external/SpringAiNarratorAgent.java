package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import java.util.List;
import java.util.Locale;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.application.ports.out.external.NarratorAgent;
import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.domain.enums.AudioFragmentKind;
import es.vargontoc.storyteller.domain.model.AudioFragment;
import es.vargontoc.storyteller.domain.model.PageAudioScript;
import es.vargontoc.storyteller.shared.Constants;
import es.vargontoc.storyteller.shared.exceptions.AppException;

/**
 * Adaptador del narrador de audio sobre Ollama (modelo {@code app.agents.audio-narrator}, independiente del
 * narrador antiguo). Las reglas viven en {@code prompts/narrator.modelfile}; aquí solo se envían los textos
 * y se valida la forma del guion. Cualquier respuesta inutilizable o fallo se devuelve como {@link AppException},
 * sin reintento. No contacta con Ollama al construirse.
 */
@Component
public class SpringAiNarratorAgent implements NarratorAgent {

    private final OllamaPort ollama;
    private final String model;
    private final ChatClient client;

    public SpringAiNarratorAgent(
            OllamaPort ollama,
            @Qualifier(Constants.BeanNames.AGENT_AUDIO_NARRATOR_MODEL) String model,
            @Qualifier(Constants.BeanNames.AGENT_AUDIO_NARRATOR) ChatClient client) {
        this.ollama = ollama;
        this.model = model;
        this.client = client;
    }

    @Override
    public List<PageAudioScript> narrate(List<String> pageTexts) {
        if (pageTexts == null || pageTexts.isEmpty()) {
            throw new AppException("No hay páginas que narrar", HttpStatus.BAD_REQUEST);
        }
        ScriptResponse response = call(describe(pageTexts));
        return toScripts(response, pageTexts.size());
    }

    private String describe(List<String> pageTexts) {
        var message = new StringBuilder("Texto aceptado de las páginas interiores:\n");
        for (int i = 0; i < pageTexts.size(); i++) {
            message.append("Página ").append(i + 1).append(": ").append(pageTexts.get(i)).append('\n');
        }
        return message.toString();
    }

    private ScriptResponse call(String message) {
        if (!ollama.isAvailable(model)) {
            throw new AppException("El agente narrador no está disponible", HttpStatus.BAD_GATEWAY);
        }
        try {
            return client.prompt().user(message).call().entity(ScriptResponse.class);
        } catch (RuntimeException e) {
            throw new AppException("El agente narrador falló: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    /** Exige una página por texto recibido, en orden, con texto, tipo y los tres parámetros por fragmento. */
    private List<PageAudioScript> toScripts(ScriptResponse response, int expectedPages) {
        if (response == null || response.pages() == null || response.pages().size() != expectedPages) {
            throw invalidResponse();
        }
        var scripts = new java.util.ArrayList<PageAudioScript>();
        for (int i = 0; i < expectedPages; i++) {
            PageResponse page = response.pages().get(i);
            if (page == null || page.page() == null || page.page() != i + 1
                    || page.fragments() == null || page.fragments().isEmpty()) {
                throw invalidResponse();
            }
            var fragments = new java.util.ArrayList<AudioFragment>();
            for (FragmentResponse f : page.fragments()) {
                if (f == null || f.text() == null || f.text().isBlank() || f.exaggeration() == null
                        || f.cfgWeight() == null || f.temperature() == null
                        || f.kind() == null || f.kind().isBlank()) {
                    throw invalidResponse();
                }
                fragments.add(new AudioFragment(f.text().trim(), f.exaggeration(), f.cfgWeight(),
                        f.temperature(), parseKind(f.kind())));
            }
            scripts.add(new PageAudioScript(i + 1, List.copyOf(fragments)));
        }
        return List.copyOf(scripts);
    }

    private AudioFragmentKind parseKind(String kind) {
        try {
            return AudioFragmentKind.valueOf(kind.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw invalidResponse();
        }
    }

    private AppException invalidResponse() {
        return new AppException("El agente narrador devolvió un guion incompleto", HttpStatus.BAD_GATEWAY);
    }

    /** Forma JSON del modelo; los números son objetos para detectar los que faltan. */
    public record ScriptResponse(List<PageResponse> pages) {
    }

    public record PageResponse(Integer page, List<FragmentResponse> fragments) {
    }

    public record FragmentResponse(String text, Double exaggeration, Double cfgWeight, Double temperature,
            String kind) {

        public FragmentResponse(String text, Double exaggeration, Double cfgWeight, Double temperature) {
            this(text, exaggeration, cfgWeight, temperature, "NARRATION");
        }
    }
}
