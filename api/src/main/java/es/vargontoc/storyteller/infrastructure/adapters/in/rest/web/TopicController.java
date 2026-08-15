package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import es.vargontoc.storyteller.application.ports.in.persistence.TopicUseCase;
import es.vargontoc.storyteller.domain.model.Topic;
import es.vargontoc.storyteller.shared.ApiResponse;

import org.springframework.web.bind.annotation.GetMapping;



@Controller
@RequestMapping("/api/v1/topic")
public class TopicController {
    
    private final TopicUseCase useCase;

    public TopicController(TopicUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<List<Topic>>> getTopics() {
        return ResponseEntity.ok(ApiResponse.ok(useCase.getTopics()));
    }
    
    
}
