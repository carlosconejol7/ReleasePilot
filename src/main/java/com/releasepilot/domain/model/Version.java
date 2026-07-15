package com.releasepilot.domain.model;

import com.releasepilot.domain.exception.DomainException;

/**
 * Value Object representing the version identifier of an Application release.
 *
 * <p>Ensures that the wrapped value is never {@code null} or blank.</p>
 *
 * @param value the underlying version value
 */
public record Version(String value) {

    public Version {
        if (value == null || value.isBlank()) {
            throw new DomainException("Version value must not be null or blank");
        }
    }
}
