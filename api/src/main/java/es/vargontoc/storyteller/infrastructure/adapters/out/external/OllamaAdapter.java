package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import java.util.List;

import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;

import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.shared.Constants;

@Component
public class OllamaAdapter implements OllamaPort {

    private final RestClient restClient;
    private final OllamaApi api;

    private final List<String> models;
    public OllamaAdapter(@Value("${spring.ai.ollama.base-url}") String url, 
        OllamaApi api,
        @Qualifier(Constants.BeanNames.AGENT_TOPICS_MODEL) String topicModel,
        @Qualifier(Constants.BeanNames.AGENT_DIRECTOR_MODEL) String directorModel,
        @Qualifier(Constants.BeanNames.AGENT_SCRIPTWRITER_MODEL) String scriptwriterModel) {
        restClient =  RestClient.builder().baseUrl(url).build();
        this.api = api;

        models = List.of(topicModel, directorModel, scriptwriterModel);
    }

    public boolean isAvailableService(){
        try {
            restClient.get().uri("/api/version").retrieve().toBodilessEntity();
            return true;
        }catch(Exception e) {
            return false;
        }
    }

    @Override
    public boolean isAvailable(String model) {
        if(!isAvailableService())
            return false;

        try {
            return api.listModels().models().stream().anyMatch(m -> m.name().equals(model) || m.name().startsWith(model + ":"));
        }catch(Exception e) {
            return false;
        }
    }

    @Override
    public void stopAllServices() {

        models.forEach(m -> {
            try {
                restClient.post().uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new OllamaUnloadModel(m))
                    .retrieve()
                    .toBodilessEntity();
            }catch(Exception e){

            }
        });

    }

    
    record OllamaUnloadModel(String model, @JsonProperty("keep_alive") int keepAlive){
        public OllamaUnloadModel(String model){
            this(model, 0);
        }
    }
}
