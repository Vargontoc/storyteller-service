package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.vargontoc.storyteller.application.ports.in.generator.StoryNarrationUseCase;
import es.vargontoc.storyteller.domain.response.NarrationState;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto.StartNarrationRequestDto;
import es.vargontoc.storyteller.shared.ApiResponse;
import es.vargontoc.storyteller.shared.exceptions.AppException;

/**
 * Narracion de un cuento: inicio/renarracion asincrono, estado y audio de una pagina interior.
 * Los rechazos de inicio responden 409 con el motivo estable ({@code NarrationRejectionReason}) en
 * {@code errors[0]}; ver {@code GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/v1/storyteller/{storyId}")
public class StoryNarrationController {

    /** Valor reservado de {@code voiceId} para la voz del sistema; equivale a id nulo en el servicio. */
    static final String SYSTEM_VOICE_ID = "system";

    private static final MediaType AUDIO_WAV = MediaType.parseMediaType("audio/wav");

    private final StoryNarrationUseCase narration;

    public StoryNarrationController(StoryNarrationUseCase narration) {
        this.narration = narration;
    }

    @PostMapping("/narration")
    public ResponseEntity<ApiResponse<NarrationState>> start(@PathVariable("storyId") Long storyId,
            @RequestBody(required = false) StartNarrationRequestDto request) {
        Long voiceId = parseVoiceId(request == null ? null : request.voiceId());
        return ResponseEntity.accepted().body(ApiResponse.ok(narration.start(storyId, voiceId)));
    }

    @GetMapping("/narration")
    public ResponseEntity<ApiResponse<NarrationState>> state(@PathVariable("storyId") Long storyId) {
        return ResponseEntity.ok(ApiResponse.ok(narration.getState(storyId)));
    }

    /** Audio WAV de una pagina interior por su numero (1..n); portada o sin audio: 404. */
    @GetMapping("/pages/{number}/audio")
    public ResponseEntity<byte[]> pageAudio(@PathVariable("storyId") Long storyId,
            @PathVariable("number") int number) {
        return ResponseEntity.ok().contentType(AUDIO_WAV).body(narration.getPageAudio(storyId, number));
    }

    private static Long parseVoiceId(String voiceId) {
        if (voiceId == null || voiceId.isBlank()) {
            throw new AppException("voiceId es obligatorio: id numerico de la voz o 'system'", HttpStatus.BAD_REQUEST);
        }
        if (SYSTEM_VOICE_ID.equals(voiceId)) {
            return null;
        }
        try {
            return Long.valueOf(voiceId);
        } catch (NumberFormatException e) {
            throw new AppException("voiceId debe ser un id numerico o 'system'", HttpStatus.BAD_REQUEST);
        }
    }
}
