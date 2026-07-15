package com.releasepilot.domain.model;

import com.releasepilot.domain.exception.DomainException;

/**
 * Value Object representing the unique identifier of a Promotion.
 *
 * <p>Ensures that the wrapped value is never {@code null} or blank.</p>
 *
 * @param value the underlying identifier value
 */
public record PromotionId(String value) {

    public PromotionId {
        if (value == null || value.isBlank()) {
            throw new DomainException("PromotionId value must not be null or blank");
        }
    }
}
