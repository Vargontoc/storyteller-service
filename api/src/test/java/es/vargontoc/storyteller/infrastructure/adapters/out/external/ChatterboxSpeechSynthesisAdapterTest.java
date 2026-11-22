package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import es.vargontoc.storyteller.domain.request.SpeechSynthesisRequest;
import es.vargontoc.storyteller.shared.exceptions.AppException;

class ChatterboxSpeechSynthesisAdapterTest {

    private static final String URL = "http://chatterbox/audio/speech/upload";
    private static final byte[] SAMPLE = "MUESTRA-DE-REFERENCIA".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WAV = "RIFF-audio".getBytes(StandardCharsets.US_ASCII);

    private MockRestServiceServer server;
    private ChatterboxSpeechSynthesisAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://chatterbox");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new ChatterboxSpeechSynthesisAdapter(builder.build());
    }

    @Test
    void sendsTextLanguageSampleAndExpressivenessAndReturnsTheAudio() {
        AtomicReference<String> body = new AtomicReference<>();
        server.expect(requestTo(URL)).andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", org.hamcrest.Matchers.startsWith("multipart/form-data")))
                .andExpect(request -> body.set(((MockClientHttpRequest) request).getBodyAsString()))
                .andRespond(withSuccess(WAV, MediaType.parseMediaType("audio/wav")));

        byte[] audio = adapter.synthesize(new SpeechSynthesisRequest("¡Guau! dijo el perro", "es", SAMPLE, 0.9, 0.4, 0.8));

        assertArrayEquals(WAV, audio);
        server.verify();
        assertField(body.get(), "input", "¡Guau! dijo el perro");
        assertField(body.get(), "language_id", "es");
        assertField(body.get(), "exaggeration", "0.9");
        assertField(body.get(), "cfg_weight", "0.4");
        assertField(body.get(), "temperature", "0.8");
        assertTrue(body.get().contains("name=\"voice_file\""));
        assertTrue(body.get().contains("MUESTRA-DE-REFERENCIA"));
    }

    @Test
    void clampsExpressivenessToTheNarratorRanges() {
        AtomicReference<String> body = new AtomicReference<>();
        server.expect(requestTo(URL))
                .andExpect(request -> body.set(((MockClientHttpRequest) request).getBodyAsString()))
                .andRespond(withSuccess(WAV, MediaType.parseMediaType("audio/wav")));

        adapter.synthesize(new SpeechSynthesisRequest("Hola", "es", SAMPLE, 9.0, -1.0, 0.0));

        assertField(body.get(), "exaggeration", "2.0");
        assertField(body.get(), "cfg_weight", "0.0");
        assertField(body.get(), "temperature", "0.05");
    }

    @Test
    void engineErrorIsAnExplicitDomainError() {
        server.expect(requestTo(URL)).andRespond(withServerError());

        AppException e = assertThrows(AppException.class, () -> adapter.synthesize(request()));
        assertEquals(HttpStatus.BAD_GATEWAY, e.getStatus());
    }

    @Test
    void timeoutIsAnExplicitDomainError() {
        server.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException("read timed out")));

        AppException e = assertThrows(AppException.class, () -> adapter.synthesize(request()));
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, e.getStatus());
    }

    @Test
    void emptyResponseIsAnExplicitDomainError() {
        server.expect(requestTo(URL)).andRespond(withSuccess(new byte[0], MediaType.parseMediaType("audio/wav")));

        AppException e = assertThrows(AppException.class, () -> adapter.synthesize(request()));
        assertEquals(HttpStatus.BAD_GATEWAY, e.getStatus());
    }

    @Test
    void requestWithoutTextOrSampleIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SpeechSynthesisRequest(" ", "es", SAMPLE, 0.5, 0.5, 0.8));
        assertThrows(IllegalArgumentException.class, () -> new SpeechSynthesisRequest("Hola", "es", new byte[0], 0.5, 0.5, 0.8));
    }

    private static SpeechSynthesisRequest request() {
        return new SpeechSynthesisRequest("Hola", "es", SAMPLE, 0.5, 0.5, 0.8);
    }

    private static void assertField(String body, String name, String value) {
        int start = body.indexOf("name=\"" + name + "\"");
        assertTrue(start >= 0, "falta el campo " + name);
        int end = body.indexOf("--", start);
        assertTrue(body.substring(start, end).contains(value), "campo " + name + " sin " + value + ": " + body.substring(start, end));
    }
}
