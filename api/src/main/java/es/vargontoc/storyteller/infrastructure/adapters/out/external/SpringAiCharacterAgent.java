package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import es.vargontoc.framework.ai.agent.AgentRequest;
import es.vargontoc.storyteller.application.ports.out.external.CharacterAgent;
import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.domain.request.CatalogCharacterReviewAgentRequest;
import es.vargontoc.storyteller.domain.response.CatalogCharacterAgentResult;
import es.vargontoc.storyteller.domain.response.CatalogCharacterReviewAgentResult;
import es.vargontoc.storyteller.shared.Constants;
import es.vargontoc.storyteller.shared.exceptions.AppException;

@Component
public class SpringAiCharacterAgent implements CharacterAgent {

    private static final String AUTOMATIC_REQUEST = "Genera una ficha de personaje apta para el catálogo.";

    private final OllamaPort ollama;
    private final String model;
    private final ChatClient client;
    private final String audience;
    private final String allowedSpecies;
    private final Resource generatePrompt;
    private final Resource reviewPrompt;

    public SpringAiCharacterAgent(
            OllamaPort ollama,
            @Qualifier(Constants.BeanNames.AGENT_CHARACTER_MODEL) String model,
            @Qualifier(Constants.BeanNames.AGENT_CHARACTER) ChatClient client,
            @Value("${app.agents.character-rules.audience}") String audience,
            @Value("${app.agents.character-rules.allowed-species}") String allowedSpecies,
            @Value("classpath:/prompts/character_generate.st") Resource generatePrompt,
            @Value("classpath:/prompts/character_review.st") Resource reviewPrompt) {
        this.ollama = ollama;
        this.model = model;
        this.client = client;
        this.audience = audience;
        this.allowedSpecies = allowedSpecies;
        this.generatePrompt = generatePrompt;
        this.reviewPrompt = reviewPrompt;
    }

    @Override
    public CatalogCharacterAgentResult generateAutomatically() {
        return generate(new AgentRequest(AUTOMATIC_REQUEST));
    }

    @Override
    public CatalogCharacterAgentResult generateFromHint(String hint) {
        return generate(new AgentRequest(hint));
    }

    @Override
    public CatalogCharacterReviewAgentResult review(CatalogCharacterReviewAgentRequest request) {
        AgentRequest hint = new AgentRequest(request.hint());
        ensureAvailable();
        CatalogCharacterReviewAgentResult result = client.prompt()
                .user(user -> user.text(reviewPrompt)
                        .param("audience", audience)
                        .param("allowedSpecies", allowedSpecies)
                        .param("name", request.name())
                        .param("narrativeDescription", request.narrativeDescription())
                        .param("visualDescription", request.visualDescription())
                        .param("visualDescriptionEn", request.visualDescriptionEn())
                        .param("visualAttributes", String.join(", ", attributesOf(request.visualAttributes())))
                        .param("target", request.target().name())
                        .param("hint", hint.message()))
                .call()
                .entity(CatalogCharacterReviewAgentResult.class);
        return validateReview(result);
    }

    private CatalogCharacterAgentResult generate(AgentRequest request) {
        ensureAvailable();
        CatalogCharacterAgentResult result = client.prompt()
                .user(user -> user.text(generatePrompt)
                        .param("audience", audience)
                        .param("allowedSpecies", allowedSpecies)
                        .param("hint", request.message()))
                .call()
                .entity(CatalogCharacterAgentResult.class);
        return validateGeneration(result);
    }

    private void ensureAvailable() {
        if (!ollama.isAvailable(model)) {
            throw new AppException("El agente de personaje no está disponible", HttpStatus.BAD_REQUEST);
        }
    }

    private CatalogCharacterAgentResult validateGeneration(CatalogCharacterAgentResult result) {
        if (result == null) {
            throw invalidResponse();
        }
        if (!result.accepted()) {
            if (isBlank(result.rejectionReason())) {
                throw invalidResponse();
            }
            return result;
        }
        if (isBlank(result.name()) || isBlank(result.narrativeDescription()) || isBlank(result.visualDescription())) {
            throw invalidResponse();
        }
        return result;
    }

    private CatalogCharacterReviewAgentResult validateReview(CatalogCharacterReviewAgentResult result) {
        if (result == null) {
            throw invalidResponse();
        }
        if (!result.accepted()) {
            if (isBlank(result.rejectionReason())) {
                throw invalidResponse();
            }
            return result;
        }
        if (isBlank(result.name()) || isBlank(result.narrativeDescription()) || isBlank(result.visualDescription())) {
            throw invalidResponse();
        }
        return result;
    }

    private AppException invalidResponse() {
        return new AppException("El agente de personaje devolvió una respuesta incompleta", HttpStatus.BAD_REQUEST);
    }

    private List<String> attributesOf(List<String> attributes) {
        return attributes == null ? List.of() : attributes;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
