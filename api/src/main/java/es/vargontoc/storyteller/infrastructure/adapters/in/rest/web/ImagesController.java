package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.vargontoc.storyteller.application.ports.in.generator.ImageGeneration;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto.GenerateImageRequestDto;
import es.vargontoc.storyteller.shared.ApiResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/v1/images")
public class ImagesController {
    
    private final ImageGeneration generation;
    public ImagesController(ImageGeneration generation) {
        this.generation = generation;
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<byte[]>> generateImage(@RequestBody GenerateImageRequestDto request) {
        return ResponseEntity.ok(ApiResponse.ok(generation.generateImage(request.image(), request.id())));
    }
    
}
