package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import es.vargontoc.storyteller.application.ports.out.external.AudioGeneratorPort;
import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.domain.request.AudioGenerationRequest;
import es.vargontoc.storyteller.infrastructure.config.ChatterboxProperties;

@Component
public class AudioGeneratorAdapter implements AudioGeneratorPort {


    private final OllamaPort ollama;
    private final RestClient restClient;

    
    public AudioGeneratorAdapter(ChatterboxProperties properties, OllamaPort ollama) {
        this.ollama = ollama;
        this.restClient = RestClient.builder()
            .baseUrl(properties.baseUrl())
            .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                setConnectTimeout((int) Duration.ofSeconds(properties.timeoutSeconds()).toMillis());
                setReadTimeout((int) Duration.ofSeconds(properties.timeoutSeconds()).toMillis());
            }})
            .build();
    }


    @Override
    public byte[] generateAudio(AudioGenerationRequest request) {

        ollama.stopAllServices();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("input", request.text());
        body.put("voice", request.voiceName());
        body.put("response_format", "wav");
        body.put("exaggeration", request.exageration());
        body.put("cfg_weight", request.cfgWeight());
        body.put("temperature", request.temperature());

        return restClient.post().uri("/audio/speech").contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(byte[].class);
    }
    
}
