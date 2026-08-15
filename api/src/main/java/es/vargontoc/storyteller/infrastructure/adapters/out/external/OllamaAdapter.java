package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;

@Component
public class OllamaAdapter implements OllamaPort {

    private final OllamaApi api;

    public OllamaAdapter(OllamaApi api) {
        this.api = api;
    }

    @Override
    public boolean isAvailable(String model) {
        try {
            return api.listModels().models().stream().anyMatch(m -> m.name().equals(model) || m.name().startsWith(model + ":"));
        }catch(Exception e) {
            return false;
        }
    }

    @Override
    public void stopAllServices() {
        // TODO:
    }
    
}
