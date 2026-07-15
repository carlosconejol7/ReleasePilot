package com.releasepilot.domain.exception;

/**
 * Base exception type for all domain-related errors within the ReleasePilot application.
 *
 * <p>This exception should be extended by more specific domain exceptions that represent
 * violations of business rules or invariants within the domain model.</p>
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
