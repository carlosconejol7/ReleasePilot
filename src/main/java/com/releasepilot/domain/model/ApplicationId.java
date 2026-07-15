package com.releasepilot.domain.model;

import com.releasepilot.domain.exception.DomainException;

/**
 * Value Object representing the unique identifier of an Application.
 *
 * <p>Ensures that the wrapped value is never {@code null} or blank.</p>
 *
 * @param value the underlying identifier value
 */
public record ApplicationId(String value) {

    public ApplicationId {
        if (value == null || value.isBlank()) {
            throw new DomainException("ApplicationId value must not be null or blank");
        }
    }
}
