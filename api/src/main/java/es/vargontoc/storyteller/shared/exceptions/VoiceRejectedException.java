package es.vargontoc.storyteller.shared.exceptions;

import org.springframework.http.HttpStatus;

import es.vargontoc.storyteller.domain.enums.VoiceRejectionReason;

/** Subida de voz rechazada. El motivo es estable: el controller lo traduce a su respuesta. */
public class VoiceRejectedException extends AppException {

    private final VoiceRejectionReason reason;

    public VoiceRejectedException(VoiceRejectionReason reason, String message) {
        super(message, statusOf(reason));
        this.reason = reason;
    }

    private static HttpStatus statusOf(VoiceRejectionReason reason) {
        return switch (reason) {
            case NAME_DUPLICATED, SYSTEM_VOICE_NOT_DELETABLE -> HttpStatus.CONFLICT;
            case VOICE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    public VoiceRejectionReason getReason() {
        return reason;
    }
}
