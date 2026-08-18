package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import es.vargontoc.storyteller.application.ports.out.external.ComfyUIPort;
import es.vargontoc.storyteller.infrastructure.config.ComfyUIProperties;

@Component
public class ComfyUIAdapter implements ComfyUIPort {

    protected final ComfyUIProperties properties;
    protected final RestClient restClient;

    

    public ComfyUIAdapter(ComfyUIProperties properties) {
        this.restClient = RestClient.builder().baseUrl(properties.baseUrl()).build();
        this.properties = properties;
    }



    @Override
    public boolean isAvailableService() {
        try {
            restClient.get().uri("/api/system_stats").retrieve().toBodilessEntity();
            return true;
        }catch(Exception e) {
                return false;
        }
    }
    
}
