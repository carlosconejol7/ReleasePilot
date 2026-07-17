package com.releasepilot.domain.model;

import com.releasepilot.domain.exception.DomainException;

/**
 * Value Object representing the identity of a user with the authority to approve a Promotion.
 *
 * <p>Ensures that the wrapped value is never {@code null} or blank.</p>
 *
 * @param value the underlying approver identifier value
 */
public record Approver(String value) {

    public Approver {
        if (value == null || value.isBlank()) {
            throw new DomainException("Approver value must not be null or blank");
        }
    }
}
