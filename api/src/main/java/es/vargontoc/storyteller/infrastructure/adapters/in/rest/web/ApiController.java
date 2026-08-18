package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.vargontoc.storyteller.application.ports.in.ApiUseCase;
import es.vargontoc.storyteller.domain.response.ApiStateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/v1")
@Tag(name =  "api")
public class ApiController {
    
    private final ApiUseCase useCase;
    public ApiController(ApiUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/services")
    @Operation(description =  "Obtiene el estado de los servicios externos de la aplicacion")
    public ResponseEntity<ApiStateResponse> getStatusService() {
        return ResponseEntity.ok(useCase.getServicesStatus());
    }
    
}
