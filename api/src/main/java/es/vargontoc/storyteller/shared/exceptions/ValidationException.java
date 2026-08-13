package es.vargontoc.storyteller.shared.exceptions;

import org.springframework.http.HttpStatus;

public class ValidationException extends AppException {

    public ValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
