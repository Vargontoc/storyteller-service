package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import org.springframework.web.client.RestClient;

import es.vargontoc.storyteller.application.ports.out.external.ChatterboxPort;
import es.vargontoc.storyteller.infrastructure.config.ChatterboxProperties;

public abstract class ChatterboxAdapter implements ChatterboxPort {

    protected final RestClient restClient;
    protected final ChatterboxProperties properties;

    public ChatterboxAdapter(ChatterboxProperties properties) {
        this.restClient = RestClient.builder().baseUrl(properties.baseUrl()).build();
        this.properties = properties;
        
    }

    @Override
    public boolean isAvailableService() {
        try {
                restClient.get().uri("/ping").retrieve().toBodilessEntity();
                return true;
        }catch(Exception e) {
                return false;
        }
    }
}
