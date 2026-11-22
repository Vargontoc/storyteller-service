package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;

import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.domain.model.PageAudioScript;
import es.vargontoc.storyteller.infrastructure.adapters.out.external.SpringAiNarratorAgent.FragmentResponse;
import es.vargontoc.storyteller.infrastructure.adapters.out.external.SpringAiNarratorAgent.PageResponse;
import es.vargontoc.storyteller.infrastructure.adapters.out.external.SpringAiNarratorAgent.ScriptResponse;
import es.vargontoc.storyteller.shared.exceptions.AppException;

class SpringAiNarratorAgentTest {

    private static final String MODEL = "storyteller-audio-narrator";

    private final OllamaPort ollama = mock(OllamaPort.class);
    private final ChatClient client = mock(ChatClient.class);
    private final ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class);
    private final ChatClient.CallResponseSpec response = mock(ChatClient.CallResponseSpec.class);
    private SpringAiNarratorAgent agent;

    @BeforeEach
    void setUp() {
        when(ollama.isAvailable(MODEL)).thenReturn(true);
        when(client.prompt()).thenReturn(request);
        when(request.user(anyString())).thenReturn(request);
        when(request.call()).thenReturn(response);
        agent = new SpringAiNarratorAgent(ollama, MODEL, client);
    }

    @Test
    void requestCarriesTheEightInteriorTextsInOrder() {
        when(response.entity(ScriptResponse.class)).thenReturn(validScript(8));

        agent.narrate(texts(8));

        String message = sentMessage();
        int previous = -1;
        for (int i = 1; i <= 8; i++) {
            int at = message.indexOf("Página " + i + ": Texto de la página " + i + ".");
            assertTrue(at > previous, "falta o está desordenada la página " + i);
            previous = at;
        }
        assertFalse(message.toLowerCase().contains("portada"));
        assertFalse(message.contains("Página 9"));
    }

    @Test
    void validResponseBecomesOneScriptPerPageWithFragmentsAndParameters() {
        when(response.entity(ScriptResponse.class)).thenReturn(validScript(8));

        List<PageAudioScript> scripts = agent.narrate(texts(8));

        assertEquals(8, scripts.size());
        for (int i = 0; i < 8; i++) {
            assertEquals(i + 1, scripts.get(i).page());
            assertEquals(2, scripts.get(i).fragments().size());
        }
        var first = scripts.get(0).fragments().get(0);
        assertEquals("Texto de la página 1.", first.text());
        assertEquals(0.9, first.exaggeration());
        assertEquals(0.5, first.cfgWeight());
        assertEquals(0.8, first.temperature());
    }

    @Test
    void malformedResponseIsAnError() {
        when(response.entity(ScriptResponse.class)).thenReturn(null);
        assertThrows(AppException.class, () -> agent.narrate(texts(8)));

        when(response.entity(ScriptResponse.class)).thenReturn(new ScriptResponse(null));
        assertThrows(AppException.class, () -> agent.narrate(texts(8)));

        when(response.entity(ScriptResponse.class)).thenThrow(new IllegalStateException("JSON inválido"));
        assertThrows(AppException.class, () -> agent.narrate(texts(8)));
    }

    @Test
    void responseMissingAPageIsAnError() {
        when(response.entity(ScriptResponse.class)).thenReturn(validScript(7));

        assertThrows(AppException.class, () -> agent.narrate(texts(8)));
    }

    @Test
    void responseWithWrongPageNumberOrWithoutFragmentsIsAnError() {
        List<PageResponse> pages = new ArrayList<>(validScript(8).pages());
        pages.set(3, new PageResponse(9, pages.get(3).fragments()));
        when(response.entity(ScriptResponse.class)).thenReturn(new ScriptResponse(pages));
        assertThrows(AppException.class, () -> agent.narrate(texts(8)));

        pages.set(3, new PageResponse(4, List.of()));
        assertThrows(AppException.class, () -> agent.narrate(texts(8)));
    }

    @Test
    void fragmentWithoutParametersOrTextIsAnError() {
        for (FragmentResponse bad : List.of(
                new FragmentResponse("Texto.", null, 0.5, 0.8),
                new FragmentResponse("Texto.", 0.9, null, 0.8),
                new FragmentResponse("Texto.", 0.9, 0.5, null),
                new FragmentResponse(" ", 0.9, 0.5, 0.8))) {
            when(response.entity(ScriptResponse.class)).thenReturn(
                    new ScriptResponse(List.of(new PageResponse(1, List.of(bad)))));

            assertThrows(AppException.class, () -> agent.narrate(texts(1)));
        }
    }

    @Test
    void ollamaFailureIsAnErrorWithoutRetry() {
        when(response.entity(ScriptResponse.class)).thenThrow(new IllegalStateException("conexión rechazada"));

        assertThrows(AppException.class, () -> agent.narrate(texts(8)));

        verify(request).call();
    }

    @Test
    void unavailableModelIsAnError() {
        when(ollama.isAvailable(MODEL)).thenReturn(false);

        assertThrows(AppException.class, () -> agent.narrate(texts(8)));
    }

    private String sentMessage() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(request).user(captor.capture());
        return captor.getValue();
    }

    private List<String> texts(int count) {
        List<String> texts = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            texts.add("Texto de la página " + i + ".");
        }
        return texts;
    }

    private ScriptResponse validScript(int pages) {
        List<PageResponse> list = new ArrayList<>();
        for (int i = 1; i <= pages; i++) {
            list.add(new PageResponse(i, List.of(
                    new FragmentResponse("Texto de la página " + i + ".", 0.9, 0.5, 0.8),
                    new FragmentResponse("Y sigue.", 0.6, 0.4, 0.8))));
        }
        return new ScriptResponse(list);
    }
}
