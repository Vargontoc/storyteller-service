package es.vargontoc.storyteller.shared.exceptions;

import org.springframework.http.HttpStatus;

import es.vargontoc.storyteller.domain.enums.NarrationRejectionReason;

/** Inicio de narracion rechazado (409). El motivo es estable: el controller lo traduce a su respuesta. */
public class NarrationRejectedException extends AppException {

    private final NarrationRejectionReason reason;

    public NarrationRejectedException(NarrationRejectionReason reason, String message) {
        super(message, HttpStatus.CONFLICT);
        this.reason = reason;
    }

    public NarrationRejectionReason getReason() {
        return reason;
    }
}
