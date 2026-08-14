package es.vargontoc.storyteller.shared.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import es.vargontoc.storyteller.shared.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final String INTERNAL_ERROR_MESSAGE = "An unexpected error occurred";
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final Marker EXCEPTION_MARKER = MarkerFactory.getMarker("EXCEPTION_MARKER");
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
            ResourceNotFoundException exception, HttpServletRequest request) {
        logDomainException(exception, request);
        return ResponseEntity
            .status(exception.getStatus())
            .body(ApiResponse.error(exception.getMessage()));
    }

    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            ValidationException exception, HttpServletRequest request) {
        logDomainException(exception, request);
        return ResponseEntity
            .status(exception.getStatus())
            .body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(
            AppException exception, HttpServletRequest request) {
        logDomainException(exception, request);
        return ResponseEntity
            .status(exception.getStatus())
            .body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnhandledException(
            Exception exception, HttpServletRequest request) {
        putRequestContext(request, 500, exception.getClass().getSimpleName());
    
        LOGGER.error(EXCEPTION_MARKER, "Unhandled exception at {} {}", request.getMethod(), request.getRequestURI(), exception);
        MDC.clear();
        return ResponseEntity
            .internalServerError()
            .body(ApiResponse.error(INTERNAL_ERROR_MESSAGE));
    }

    
    private void logDomainException(AppException exception, HttpServletRequest request) {
        putRequestContext(request, exception.getStatus().value(), exception.getClass().getSimpleName());
        LOGGER.warn(EXCEPTION_MARKER, "Domain exception at {} {}: {}",
            request.getMethod(), request.getRequestURI(), exception.getMessage());
        MDC.clear();
    }

    private void putRequestContext(HttpServletRequest request, int httpStatus, String exceptionType) {
        MDC.put("requestUri", request.getRequestURI());
        MDC.put("requestMethod", request.getMethod());
        MDC.put("httpStatus", String.valueOf(httpStatus));
        MDC.put("exceptionType", exceptionType);
    }
}
