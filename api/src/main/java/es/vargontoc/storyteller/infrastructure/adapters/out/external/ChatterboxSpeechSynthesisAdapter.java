package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import java.net.http.HttpClient;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import es.vargontoc.storyteller.application.ports.out.external.SpeechSynthesizer;
import es.vargontoc.storyteller.domain.request.SpeechSynthesisRequest;
import es.vargontoc.storyteller.infrastructure.config.ChatterboxProperties;
import es.vargontoc.storyteller.shared.exceptions.AppException;

/**
 * Síntesis con Chatterbox Multilingual V3 para el flujo de narración con escaleta. Envía por petición
 * el texto, el idioma, la muestra de referencia y la expresividad al endpoint multipart del contenedor.
 * Independiente de {@code AudioGeneratorAdapter} (cuentos antiguos). No libera la VRAM de Ollama ni reintenta.
 */
@Component
public class ChatterboxSpeechSynthesisAdapter implements SpeechSynthesizer {

    private static final Logger log = LoggerFactory.getLogger(ChatterboxSpeechSynthesisAdapter.class);
    static final int MAX_ERROR_BODY = 300;

    static final double MIN_EXAGGERATION = 0.25, MAX_EXAGGERATION = 2.0;
    static final double MIN_CFG_WEIGHT = 0.0, MAX_CFG_WEIGHT = 1.0;
    static final double MIN_TEMPERATURE = 0.05, MAX_TEMPERATURE = 5.0;

    private final RestClient restClient;

    @Autowired
    public ChatterboxSpeechSynthesisAdapter(ChatterboxProperties properties) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                // HTTP/1.1 explícito: con el valor por defecto (HTTP/2) el JDK envía "Upgrade: h2c" y uvicorn,
                // al ignorarlo, no lee el cuerpo multipart y responde 422 "input: Field required".
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        this.restClient = RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(factory).build();
    }

    ChatterboxSpeechSynthesisAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public byte[] synthesize(SpeechSynthesisRequest request) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("input", request.text());
        form.add("language_id", request.language());
        form.add("response_format", "wav");
        form.add("exaggeration", String.valueOf(clamp(request.exaggeration(), MIN_EXAGGERATION, MAX_EXAGGERATION)));
        form.add("cfg_weight", String.valueOf(clamp(request.cfgWeight(), MIN_CFG_WEIGHT, MAX_CFG_WEIGHT)));
        form.add("temperature", String.valueOf(clamp(request.temperature(), MIN_TEMPERATURE, MAX_TEMPERATURE)));
        form.add("voice_file", new ByteArrayResource(request.referenceSample()) {
            @Override
            public String getFilename() {
                return "reference.wav";
            }
        });

        byte[] audio;
        try {
            audio = restClient.post().uri("/audio/speech/upload")
                    .contentType(MediaType.MULTIPART_FORM_DATA).body(form).retrieve().body(byte[].class);
        } catch (ResourceAccessException e) {
            throw new AppException("Chatterbox no respondió a tiempo o no está disponible", HttpStatus.GATEWAY_TIMEOUT);
        } catch (RestClientResponseException e) {
            String body = abbreviate(e.getResponseBodyAsString());
            log.error("Chatterbox respondió {} al sintetizar: {}", e.getStatusCode(), body, e);
            throw new AppException("Chatterbox falló al sintetizar el fragmento: HTTP "
                    + e.getStatusCode().value() + " " + body, HttpStatus.BAD_GATEWAY);
        } catch (RestClientException e) {
            log.error("Chatterbox falló al sintetizar el fragmento", e);
            throw new AppException("Chatterbox falló al sintetizar el fragmento", HttpStatus.BAD_GATEWAY);
        }
        if (audio == null || audio.length == 0)
            throw new AppException("Chatterbox devolvió un audio vacío", HttpStatus.BAD_GATEWAY);
        return audio;
    }

    private static String abbreviate(String body) {
        if (body == null) return "";
        String text = body.strip();
        return text.length() <= MAX_ERROR_BODY ? text : text.substring(0, MAX_ERROR_BODY) + "...";
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
