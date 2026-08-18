package es.vargontoc.storyteller.application.ports.in;

import es.vargontoc.storyteller.domain.response.ApiStateResponse;

public interface ApiUseCase {
    
    ApiStateResponse getServicesStatus();
}
