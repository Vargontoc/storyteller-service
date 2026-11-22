package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;

import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.domain.model.EpisodicCastMember;
import es.vargontoc.storyteller.domain.model.SceneComposition;
import es.vargontoc.storyteller.domain.model.StoryBeat;
import es.vargontoc.storyteller.domain.request.WriterContext;
import es.vargontoc.storyteller.domain.response.WriterHintResult;
import es.vargontoc.storyteller.domain.response.WriterResult;
import es.vargontoc.storyteller.shared.exceptions.AppException;

class SpringAiWriterAgentTest {

    private final OllamaPort ollama = mock(OllamaPort.class);
    private final ChatClient client = mock(ChatClient.class);
    private final ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class);
    private final ChatClient.CallResponseSpec response = mock(ChatClient.CallResponseSpec.class);
    private SpringAiWriterAgent agent;

    @BeforeEach
    void setUp() {
        when(ollama.isAvailable("storyteller-writer")).thenReturn(true);
        when(client.prompt()).thenReturn(request);
        when(request.user(anyString())).thenReturn(request);
        when(request.call()).thenReturn(response);
        agent = new SpringAiWriterAgent(ollama, "storyteller-writer", client);
    }

    @Test
    void coverSendsTitleSynopsisAndSnapshotWithoutBeatsOrCast() {
        when(response.entity(WriterResult.class)).thenReturn(new WriterResult(null, scene("Luna en el claro")));

        WriterResult result = agent.writeCover(WriterContext.cover("El bosque de Luna", "Luna ayuda a un amigo", "Luna, zorra naranja"));

        String message = sentMessage();
        assertTrue(message.contains("El bosque de Luna"));
        assertTrue(message.contains("Luna ayuda a un amigo"));
        assertTrue(message.contains("Luna, zorra naranja"));
        assertFalse(message.contains("Beat"));
        assertFalse(message.contains("Reparto"));
        assertNull(result.text());
        assertEquals("Luna en el claro", result.scene().scene());
    }

    @Test
    void pageSendsBeatContextAndPreviousAcceptedTexts() {
        when(response.entity(WriterResult.class)).thenReturn(new WriterResult(" Luna ve un conejo. ", scene("Luna y el conejo")));

        WriterResult result = agent.writePage(pageContext());

        String message = sentMessage();
        assertTrue(message.contains("Luna encuentra al conejo"));
        assertTrue(message.contains("El bosque de Luna"));
        assertTrue(message.contains("Luna ayuda a un amigo"));
        assertTrue(message.contains("Conejo blanco: Amigo que necesita ayuda"));
        assertTrue(message.contains("Luna, zorra naranja"));
        assertTrue(message.contains("Página 1: Luna sale."));
        assertTrue(message.contains("Página 2: Luna camina."));
        assertEquals("Luna ve un conejo.", result.text());
        assertEquals("Luna y el conejo", result.scene().scene());
    }

    @Test
    void pageRequestIncludesOnomatopoeiaRulesLinkedToTheBeat() {
        when(response.entity(WriterResult.class)).thenReturn(new WriterResult("Luna ve un conejo.", scene("Luna")));

        agent.writePage(pageContext());

        String message = sentMessage();
        assertTrue(message.contains("Luna encuentra al conejo"));
        assertTrue(message.contains("Como máximo una onomatopeya"));
        assertTrue(message.contains("sonido natural"));
        assertTrue(message.contains("beat de esta página"));
        assertTrue(message.contains("forma estándar y pronunciable"));
        assertTrue(message.contains("sin letras repetidas alargadas ni símbolos"));
    }

    @Test
    void coverRequestForbidsOnomatopoeia() {
        when(response.entity(WriterResult.class)).thenReturn(new WriterResult(null, scene("Luna")));

        agent.writeCover(WriterContext.cover("T", "S", "Luna"));

        assertTrue(sentMessage().contains("no incluyas ninguna onomatopeya"));
    }

    @Test
    void onomatopoeiaRulesDoNotChangeTheWordTarget() {
        when(response.entity(WriterResult.class)).thenReturn(new WriterResult("Texto.", scene("Luna")));

        agent.writePage(pageContext());

        String message = sentMessage();
        assertFalse(message.matches("(?s).*\\d+\\s+palabras.*"));
        assertFalse(SpringAiWriterAgent.PAGE_ONOMATOPOEIA_RULES.toLowerCase().contains("palabras"));
        assertFalse(SpringAiWriterAgent.COVER_ONOMATOPOEIA_RULE.toLowerCase().contains("palabras"));
    }

    @Test
    void acceptedHintSendsBaseAndHintAndReturnsNewContent() {
        when(response.entity(WriterHintResult.class)).thenReturn(
                new WriterHintResult(true, null, "Luna ve un conejo contento.", scene("Conejo sonriendo")));

        WriterHintResult result = agent.rewrite(pageContext(),
                new WriterResult("Luna ve un conejo.", scene("Luna y el conejo")), "que el conejo esté contento");

        String message = sentMessage();
        assertTrue(message.contains("Luna ve un conejo."));
        assertTrue(message.contains("que el conejo esté contento"));
        assertTrue(result.hintAccepted());
        assertEquals("Luna ve un conejo contento.", result.text());
        assertEquals("Conejo sonriendo", result.scene().scene());
    }

    @Test
    void rejectedHintReturnsTheReason() {
        when(response.entity(WriterHintResult.class)).thenReturn(
                new WriterHintResult(false, " El lobo no está en el beat ", null, null));

        WriterHintResult result = agent.rewrite(pageContext(),
                new WriterResult("Luna ve un conejo.", scene("Luna y el conejo")), "que aparezca el lobo");

        assertFalse(result.hintAccepted());
        assertEquals("El lobo no está en el beat", result.rejectionReason());
    }

    @Test
    void coverHintDoesNotNeedText() {
        when(response.entity(WriterHintResult.class)).thenReturn(
                new WriterHintResult(true, null, null, scene("Luna al atardecer")));

        WriterHintResult result = agent.rewrite(WriterContext.cover("T", "S", "Luna"),
                new WriterResult(null, scene("Luna")), "al atardecer");

        assertTrue(result.hintAccepted());
        assertNull(result.text());
    }

    @Test
    void rejectionWithoutReasonIsAFailure() {
        when(response.entity(WriterHintResult.class)).thenReturn(new WriterHintResult(false, " ", null, null));

        assertThrows(AppException.class, () -> agent.rewrite(pageContext(),
                new WriterResult("Texto.", scene("Escena")), "hint"));
    }

    @Test
    void acceptedHintWithoutTextIsAFailure() {
        when(response.entity(WriterHintResult.class)).thenReturn(new WriterHintResult(true, null, " ", scene("Escena")));

        assertThrows(AppException.class, () -> agent.rewrite(pageContext(),
                new WriterResult("Texto.", scene("Escena")), "hint"));
    }

    @Test
    void pageWithoutTextIsAFailure() {
        when(response.entity(WriterResult.class)).thenReturn(new WriterResult(" ", scene("Escena")));

        assertThrows(AppException.class, () -> agent.writePage(pageContext()));
    }

    @Test
    void pageWithoutSceneIsAFailure() {
        when(response.entity(WriterResult.class)).thenReturn(new WriterResult("Texto.", new SceneComposition(" ", List.of(), "")));

        assertThrows(AppException.class, () -> agent.writePage(pageContext()));
    }

    @Test
    void coverWithoutSceneIsAFailure() {
        when(response.entity(WriterResult.class)).thenReturn(new WriterResult(null, null));

        assertThrows(AppException.class, () -> agent.writeCover(WriterContext.cover("T", "S", "Luna")));
    }

    @Test
    void ollamaErrorIsAFailure() {
        when(response.entity(WriterResult.class)).thenThrow(new IllegalStateException("conexión rechazada"));

        assertThrows(AppException.class, () -> agent.writePage(pageContext()));
    }

    @Test
    void unavailableModelIsAFailure() {
        when(ollama.isAvailable("storyteller-writer")).thenReturn(false);

        assertThrows(AppException.class, () -> agent.writePage(pageContext()));
    }

    private String sentMessage() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(request).user(captor.capture());
        return captor.getValue();
    }

    private WriterContext pageContext() {
        StoryBeat beat = new StoryBeat("Luna encuentra al conejo", "Nudo", List.of("Luna", "Conejo blanco"),
                "Curioso", "Claro del bosque");
        return new WriterContext("El bosque de Luna", "Luna ayuda a un amigo", "Luna, zorra naranja",
                List.of(new EpisodicCastMember("Conejo blanco", "Amigo que necesita ayuda")), beat,
                List.of("Luna sale.", "Luna camina."));
    }

    private SceneComposition scene(String text) {
        return new SceneComposition(text, List.of(), "bosque");
    }
}
