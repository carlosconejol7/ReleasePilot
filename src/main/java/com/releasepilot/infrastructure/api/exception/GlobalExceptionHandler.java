package com.releasepilot.infrastructure.api.exception;

import com.releasepilot.domain.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * Centralized exception handling for the REST API layer.
 *
 * <p>Translates domain rule violations ({@link DomainException}) into a standardized
 * {@code 400 Bad Request} response, so that API clients receive a consistent, predictable
 * error shape regardless of which endpoint or command handler raised the violation.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles any {@link DomainException} raised while processing a request.
     *
     * @param exception the domain exception that was raised
     * @return a {@code 400 Bad Request} response containing a standardized {@link ErrorResponse}
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException exception) {
        ErrorResponse body = new ErrorResponse(exception.getMessage(), HttpStatus.BAD_REQUEST.value(), Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
