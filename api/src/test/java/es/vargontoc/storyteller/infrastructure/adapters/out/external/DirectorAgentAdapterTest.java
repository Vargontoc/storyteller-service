package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.domain.model.CoverPlan;
import es.vargontoc.storyteller.domain.request.DirectorReviewRequest;
import es.vargontoc.storyteller.domain.request.DirectorScriptRequest;
import es.vargontoc.storyteller.domain.response.DirectorBeat;
import es.vargontoc.storyteller.domain.response.DirectorBeatVerdict;
import es.vargontoc.storyteller.domain.response.DirectorCastMember;
import es.vargontoc.storyteller.domain.response.DirectorReviewResult;
import es.vargontoc.storyteller.domain.response.DirectorScriptResult;
import es.vargontoc.storyteller.shared.exceptions.AppException;

class DirectorAgentAdapterTest {

    private final OllamaPort ollama = mock(OllamaPort.class);
    private final ChatClient client = mock(ChatClient.class);
    private final ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class);
    private final ChatClient.CallResponseSpec response = mock(ChatClient.CallResponseSpec.class);
    private SpringAiDirectorAgent agent;

    @BeforeEach
    void setUp() {
        when(ollama.isAvailable("storyteller-director")).thenReturn(true);
        when(client.prompt()).thenReturn(request);
        when(request.user(org.mockito.ArgumentMatchers.anyString())).thenReturn(request);
        when(request.call()).thenReturn(response);
        agent = new SpringAiDirectorAgent(ollama, "storyteller-director", client);
    }

    @Test
    void generationReturnsTitleSynopsisCastAndOneCompleteBeatPerPage() {
        when(response.entity(DirectorScriptResult.class)).thenReturn(script(2));

        DirectorScriptResult result = agent.generateScript(scriptRequest(2));

        assertEquals("El bosque de Luna", result.title());
        assertEquals("Luna ayuda a un amigo", result.synopsis());
        assertEquals(List.of(new DirectorCastMember("Pipo", "Amigo que necesita ayuda")), result.cast());
        assertEquals(2, result.beats().size());
        assertEquals("Luna sale a pasear", result.beats().get(0).event());
    }

    @Test
    void generationDeliversTheSettingOfEachBeatAndTheCoverPlan() {
        var cover = new CoverPlan("habitación al anochecer", "calma");
        var beats = List.of(settingBeat("cocina"), settingBeat("jardín"), settingBeat("río"), settingBeat("cama"));
        when(response.entity(DirectorScriptResult.class)).thenReturn(
                new DirectorScriptResult("Título", "Sinopsis", List.of(), beats, cover));

        DirectorScriptResult result = agent.generateScript(scriptRequest(4));

        assertEquals(List.of("cocina", "jardín", "río", "cama"),
                result.beats().stream().map(DirectorBeat::setting).toList());
        assertEquals(cover, result.cover());
    }

    @Test
    void reviewDeliversTheSettingOfEachBeatAndTheCoverPlan() {
        var cover = new CoverPlan("playa", "alegre");
        var beats = List.of(settingBeat("cocina"), settingBeat("playa"));
        when(response.entity(DirectorReviewResult.class)).thenReturn(new DirectorReviewResult(
                true, null, "Título", "Sinopsis", List.of(), beats, cover));

        DirectorReviewResult result = agent.reviewScript(reviewRequest(2));

        assertEquals("cocina", result.beats().get(0).setting());
        assertEquals("playa", result.beats().get(1).setting());
        assertEquals(cover, result.cover());
    }

    @Test
    void missingSettingOrCoverPlanIsDeliveredAbsentWithoutInventingValues() {
        when(response.entity(DirectorScriptResult.class)).thenReturn(script(2));

        DirectorScriptResult result = agent.generateScript(scriptRequest(2));

        assertNull(result.beats().get(0).setting());
        assertNull(result.cover());
    }

    @Test
    void generationRejectsABeatMissingARequiredField() {
        DirectorBeat incomplete = new DirectorBeat("Luna sale", "Planteamiento", List.of("Luna"), " ", "Bosque");
        when(response.entity(DirectorScriptResult.class)).thenReturn(new DirectorScriptResult(
                "Título", "Sinopsis", List.of(), List.of(beat("Uno"), incomplete)));

        assertThrows(AppException.class, () -> agent.generateScript(scriptRequest(2)));
    }

    @Test
    void generationRejectsABeatWithoutParticipants() {
        DirectorBeat noOne = new DirectorBeat("Luna sale", "Planteamiento", List.of(), "Alegre", "Bosque");
        when(response.entity(DirectorScriptResult.class)).thenReturn(new DirectorScriptResult(
                "Título", "Sinopsis", List.of(), List.of(noOne)));

        assertThrows(AppException.class, () -> agent.generateScript(scriptRequest(1)));
    }

    @Test
    void generationRejectsAWrongNumberOfBeats() {
        when(response.entity(DirectorScriptResult.class)).thenReturn(script(1));

        assertThrows(AppException.class, () -> agent.generateScript(scriptRequest(2)));
    }

    @Test
    void reviewKeepsTheCardinalityOfTheCurrentOutline() {
        when(response.entity(DirectorReviewResult.class)).thenReturn(accepted(script(2)));

        DirectorReviewResult result = agent.reviewScript(reviewRequest(2));

        assertTrue(result.hintAccepted());
        assertNull(result.rejectionReason());
        assertEquals(2, result.beats().size());
    }

    @Test
    void reviewWithDifferentCardinalityIsRejected() {
        when(response.entity(DirectorReviewResult.class)).thenReturn(accepted(script(3)));

        assertThrows(AppException.class, () -> agent.reviewScript(reviewRequest(2)));
    }

    @Test
    void rejectedHintReturnsOnlyTheReason() {
        when(response.entity(DirectorReviewResult.class)).thenReturn(
                new DirectorReviewResult(false, "El hint no es apto para la edad", null, null, null, null));

        DirectorReviewResult result = agent.reviewScript(reviewRequest(2));

        assertFalse(result.hintAccepted());
        assertEquals("El hint no es apto para la edad", result.rejectionReason());
        assertNull(result.beats());
    }

    @Test
    void rejectedHintWithoutReasonIsInvalid() {
        when(response.entity(DirectorReviewResult.class)).thenReturn(
                new DirectorReviewResult(false, " ", null, null, null, null));

        assertThrows(AppException.class, () -> agent.reviewScript(reviewRequest(2)));
    }

    @Test
    void validEditedBeatIsAccepted() {
        when(response.entity(DirectorBeatVerdict.class)).thenReturn(new DirectorBeatVerdict(true, null));

        DirectorBeatVerdict verdict = agent.validateBeat(beat("Luna comparte su merienda"));

        assertTrue(verdict.accepted());
    }

    @Test
    void unfitEditedBeatReturnsVerdictAndReason() {
        when(response.entity(DirectorBeatVerdict.class)).thenReturn(
                new DirectorBeatVerdict(false, "Contiene violencia no apta para 3 a 4 años"));

        DirectorBeatVerdict verdict = agent.validateBeat(beat("Una pelea"));

        assertFalse(verdict.accepted());
        assertEquals("Contiene violencia no apta para 3 a 4 años", verdict.reason());
    }

    @Test
    void negativeVerdictWithoutReasonIsInvalid() {
        when(response.entity(DirectorBeatVerdict.class)).thenReturn(new DirectorBeatVerdict(false, null));

        assertThrows(AppException.class, () -> agent.validateBeat(beat("Una pelea")));
    }

    @Test
    void incompleteEditedBeatIsRejectedWithoutCallingTheModel() {
        DirectorBeat incomplete = new DirectorBeat("", "Cierre", List.of("Luna"), "Calmado", "Casa");

        assertThrows(AppException.class, () -> agent.validateBeat(incomplete));
    }

    @Test
    void unavailableDirectorIsReported() {
        when(ollama.isAvailable("storyteller-director")).thenReturn(false);

        assertThrows(AppException.class, () -> agent.generateScript(scriptRequest(2)));
    }

    private DirectorScriptRequest scriptRequest(int pages) {
        return new DirectorScriptRequest("Amistad", "Un paseo", pages,
                "Luna", "Una zorra curiosa", "Zorra naranja de cola esponjosa");
    }

    private DirectorReviewRequest reviewRequest(int pages) {
        return new DirectorReviewRequest("El bosque de Luna", "Luna ayuda a un amigo",
                List.of(new DirectorCastMember("Pipo", "Amigo que necesita ayuda")),
                script(pages).beats(), "Que el final sea en la playa");
    }

    private DirectorScriptResult script(int beats) {
        List<DirectorBeat> list = new java.util.ArrayList<>();
        for (int i = 0; i < beats; i++) {
            list.add(i == 0 ? beat("Luna sale a pasear") : beat("Luna encuentra a Pipo " + i));
        }
        return new DirectorScriptResult("El bosque de Luna", "Luna ayuda a un amigo",
                List.of(new DirectorCastMember("Pipo", "Amigo que necesita ayuda")), list);
    }

    private DirectorReviewResult accepted(DirectorScriptResult script) {
        return new DirectorReviewResult(true, null, script.title(), script.synopsis(), script.cast(), script.beats());
    }

    private DirectorBeat settingBeat(String setting) {
        return new DirectorBeat("Luna sale", "Planteamiento", List.of("Luna"), "Alegre", "Bosque", setting);
    }

    private DirectorBeat beat(String event) {
        return new DirectorBeat(event, "Planteamiento", List.of("Luna"), "Alegre", "Bosque soleado por la mañana");
    }
}
