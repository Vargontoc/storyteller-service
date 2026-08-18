package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.vargontoc.storyteller.application.ports.in.persistence.TopicUseCase;
import es.vargontoc.storyteller.domain.model.Topic;
import es.vargontoc.storyteller.shared.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;



@RestController
@RequestMapping("/api/v1/topic")
@Tag(name = "Topics")
public class TopicController {
    
    private final TopicUseCase useCase;

    public TopicController(TopicUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping()
    @Operation(description = "Obtiene listado de temas generados")
    public ResponseEntity<ApiResponse<List<Topic>>> getTopics() {
        return ResponseEntity.ok(ApiResponse.ok(useCase.getTopics()));
    }
    
    
}
