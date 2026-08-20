package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.application.ports.out.external.PromptTranslatorPort;
import es.vargontoc.storyteller.domain.model.TranslateDescription;
import es.vargontoc.storyteller.shared.Constants;

@Component
public class PromptTranslatorAdapter implements PromptTranslatorPort {
    
    @Value("classpath:prompts/translate_visual.st")
    private Resource systemPrompt;

    private static final Logger LOG = LoggerFactory.getLogger(PromptTranslatorAdapter.class);

    private final OllamaPort ollama;
    private final String model;
    private final ChatClient client;

    public PromptTranslatorAdapter(OllamaPort ollama,
            @Qualifier(Constants.BeanNames.AGENT_TRANSLATOR_MODEL) String model,
            @Qualifier(Constants.BeanNames.AGENT_TRANSLATOR) ChatClient client) {
        this.ollama = ollama;
        this.model = model;
        this.client = client;
    }

    @Override
    public String translateToEnglish(String text) {
        if (text == null || text.isBlank())
            return text;

        if (!ollama.isAvailable(model)) {
            LOG.warn("Agente traductor no disponible, se usa el texto original sin traducir");
            return text;
        }

        try {
            String translated = client.prompt()
                .user(text)
                .call().content();

            if (translated == null || translated.isBlank()) {
                LOG.warn("El agente traductor devolvió una respuesta vacía, se usa el texto original");
                return text;
            }
            return translated.trim();
        } catch (RuntimeException e) {
            LOG.warn("Fallo al traducir el prompt, se usa el texto original: {}", e.getMessage());
            return text;
        }
    }

    @Override
    public TranslateDescription translateCharacter(String text) {
        if (text == null || text.isBlank())
            return new TranslateDescription(text, List.of());

        if (!ollama.isAvailable(model)) {
            LOG.warn("Agente traductor no disponible, se usa el texto original sin traducir");
            return new TranslateDescription(text, List.of());
        }
        return client.prompt().system(systemPrompt).user(text).call().entity(TranslateDescription.class);
    }
}
