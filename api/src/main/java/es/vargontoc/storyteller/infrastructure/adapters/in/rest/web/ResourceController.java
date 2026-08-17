package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.vargontoc.storyteller.application.ports.in.generator.AudioGeneration;
import es.vargontoc.storyteller.application.ports.in.generator.ImageGeneration;
import es.vargontoc.storyteller.domain.command.AudioGenerateCommand;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto.GenerateImageRequestDto;
import es.vargontoc.storyteller.shared.ApiResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/v1/assets")
public class ResourceController {
    
    private final ImageGeneration image;
    private final AudioGeneration audio;
    public ResourceController(ImageGeneration image, AudioGeneration audio) {
        this.image = image;
        this.audio = audio;
    }

    @PostMapping("/images/generate")
    public ResponseEntity<ApiResponse<byte[]>> generateImage(@RequestBody GenerateImageRequestDto request) {
        return ResponseEntity.ok(ApiResponse.ok(image.generateImage(request.image(), request.id())));
    }

    @PostMapping("/audios/generate")
    public ResponseEntity<ApiResponse<byte[]>> generateImage(@RequestBody AudioGenerateCommand request) {
        return ResponseEntity.ok(ApiResponse.ok(audio.generateAudio(request)));
    }
    
}
