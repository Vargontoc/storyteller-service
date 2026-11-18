package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;

import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.domain.enums.CharacterReviewTarget;
import es.vargontoc.storyteller.domain.request.CatalogCharacterReviewAgentRequest;
import es.vargontoc.storyteller.domain.response.CatalogCharacterAgentResult;
import es.vargontoc.storyteller.domain.response.CatalogCharacterReviewAgentResult;
import es.vargontoc.storyteller.shared.exceptions.AppException;

class CharacterAgentAdapterTest {

    private final OllamaPort ollama = mock(OllamaPort.class);
    private final ChatClient client = mock(ChatClient.class);
    private final ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class);
    private final ChatClient.CallResponseSpec response = mock(ChatClient.CallResponseSpec.class);
    private SpringAiCharacterAgent agent;

    @BeforeEach
    void setUp() {
        when(ollama.isAvailable("storyteller-character")).thenReturn(true);
        when(client.prompt()).thenReturn(request);
        when(request.user(org.mockito.ArgumentMatchers.<Consumer<ChatClient.PromptUserSpec>>any()))
                .thenReturn(request);
        when(request.call()).thenReturn(response);
        agent = new SpringAiCharacterAgent(
                ollama,
                "storyteller-character",
                client,
                "niños y niñas de 3 a 4 años",
                "animales fáciles de dibujar",
                new ByteArrayResource("{hint}".getBytes()),
                new ByteArrayResource("{hint}".getBytes()));
    }

    @Test
    void automaticGenerationReturnsACompleteAcceptedProfile() {
        CatalogCharacterAgentResult expected = acceptedGeneration();
        when(response.entity(CatalogCharacterAgentResult.class)).thenReturn(expected);

        CatalogCharacterAgentResult result = agent.generateAutomatically();

        assertEquals("Luna", result.name());
        assertEquals("Una zorra curiosa", result.narrativeDescription());
        assertEquals("Zorra naranja de cola esponjosa", result.visualDescription());
    }

    @Test
    void manualGenerationReturnsACompleteAcceptedProfile() {
        when(response.entity(CatalogCharacterAgentResult.class)).thenReturn(acceptedGeneration());

        CatalogCharacterAgentResult result = agent.generateFromHint("Una zorra que ayuda a sus amigos");

        assertEquals("Luna", result.name());
        assertEquals(true, result.accepted());
    }

    @Test
    void rejectedGenerationReturnsOnlyTheRejectionReason() {
        CatalogCharacterAgentResult rejected = new CatalogCharacterAgentResult(
                false, "La propuesta no es apta para la edad indicada", null, null, null, null, null);
        when(response.entity(CatalogCharacterAgentResult.class)).thenReturn(rejected);

        CatalogCharacterAgentResult result = agent.generateFromHint("Una escena aterradora");

        assertEquals(false, result.accepted());
        assertEquals("La propuesta no es apta para la edad indicada", result.rejectionReason());
        assertEquals(null, result.name());
    }

    @Test
    void validReviewReturnsAProposal() {
        CatalogCharacterReviewAgentResult expected = new CatalogCharacterReviewAgentResult(
                true, null, "Luna", "Una zorra muy curiosa", "Zorra naranja sonriente",
                "Smiling orange fox", List.of("orange fur", "smile"));
        when(response.entity(CatalogCharacterReviewAgentResult.class)).thenReturn(expected);

        CatalogCharacterReviewAgentResult result = agent.review(reviewRequest());

        assertEquals(true, result.accepted());
        assertEquals("Una zorra muy curiosa", result.narrativeDescription());
    }

    @Test
    void incompleteProviderOutputIsRejected() {
        when(response.entity(CatalogCharacterAgentResult.class)).thenReturn(
                new CatalogCharacterAgentResult(true, null, "Luna", null, "Zorra naranja", null, List.of()));

        assertThrows(AppException.class, () -> agent.generateAutomatically());
    }

    @Test
    void narrativeReviewKeepsTheVisualIdentityFromTheRequest() {
        CatalogCharacterReviewAgentResult changedVisual = new CatalogCharacterReviewAgentResult(
                true, null, "Luna renombrada", "Una zorra muy curiosa y valiente",
                "Zorra azul con sombrero", "Blue fox with a hat", List.of("blue fox"));
        when(response.entity(CatalogCharacterReviewAgentResult.class)).thenReturn(changedVisual);
        CatalogCharacterReviewAgentRequest request = new CatalogCharacterReviewAgentRequest(
                "Luna", "Una zorra curiosa", "Zorra naranja de cola esponjosa",
                "Orange fox with a fluffy tail", List.of("orange fur", "fluffy tail"),
                CharacterReviewTarget.NARRATIVE, "Cuenta que ayuda a sus amigos");

        CatalogCharacterReviewAgentResult result = agent.review(request);

        assertEquals("Luna", result.name());
        assertEquals("Una zorra muy curiosa y valiente", result.narrativeDescription());
        assertEquals("Zorra naranja de cola esponjosa", result.visualDescription());
        assertEquals("Orange fox with a fluffy tail", result.visualDescriptionEn());
        assertEquals(List.of("orange fur", "fluffy tail"), result.visualAttributes());
    }

    @Test
    void visualReviewKeepsTheNarrativeFromTheRequest() {
        CatalogCharacterReviewAgentResult changedNarrative = new CatalogCharacterReviewAgentResult(
                true, null, "Luna", "Una historia distinta", "Zorra naranja sonriente",
                "Smiling orange fox", List.of("orange fox", "smile"));
        when(response.entity(CatalogCharacterReviewAgentResult.class)).thenReturn(changedNarrative);
        CatalogCharacterReviewAgentRequest request = new CatalogCharacterReviewAgentRequest(
                "Luna", "Una zorra curiosa", "Zorra naranja de cola esponjosa",
                "Orange fox with a fluffy tail", List.of("orange fur"),
                CharacterReviewTarget.VISUAL, "Hazla más sonriente");

        CatalogCharacterReviewAgentResult result = agent.review(request);

        assertEquals("Una zorra curiosa", result.narrativeDescription());
        assertEquals("Zorra naranja sonriente", result.visualDescription());
        assertEquals("Smiling orange fox", result.visualDescriptionEn());
    }

    @Test
    void visualAttributesAreTrimmedDeduplicatedAndCapped() {
        CatalogCharacterAgentResult noisy = new CatalogCharacterAgentResult(
                true, null, "Luna", "Una zorra curiosa", "Zorra naranja",
                "Orange fox", List.of("  orange fox ", "ORANGE FOX", "fluffy tail", "", "bright eyes", "blue scarf", "extra hat"));
        when(response.entity(CatalogCharacterAgentResult.class)).thenReturn(noisy);

        CatalogCharacterAgentResult result = agent.generateAutomatically();

        assertEquals(List.of("orange fox", "fluffy tail", "bright eyes", "blue scarf", "extra hat"),
                result.visualAttributes());
    }

    @Test
    void providerFailureIsNotConvertedIntoAProfile() {
        when(response.entity(CatalogCharacterAgentResult.class)).thenThrow(new IllegalStateException("provider unavailable"));

        assertThrows(IllegalStateException.class, () -> agent.generateAutomatically());
    }

    private CatalogCharacterAgentResult acceptedGeneration() {
        return new CatalogCharacterAgentResult(
                true, null, "Luna", "Una zorra curiosa", "Zorra naranja de cola esponjosa",
                "Orange fox with a fluffy tail", List.of("orange fur", "fluffy tail"));
    }

    private CatalogCharacterReviewAgentRequest reviewRequest() {
        return new CatalogCharacterReviewAgentRequest(
                "Luna", "Una zorra curiosa", "Zorra naranja de cola esponjosa",
                "Orange fox with a fluffy tail", List.of("orange fur"),
                CharacterReviewTarget.BOTH, "Hazla más sonriente");
    }
}
