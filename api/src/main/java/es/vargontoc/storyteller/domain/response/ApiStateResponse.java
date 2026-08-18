package es.vargontoc.storyteller.domain.response;

public record ApiStateResponse(OllamaApiResponse ollama, boolean chatterbox, boolean comfy) {
    
}
