package com.releasepilot.domain.model;

import com.releasepilot.domain.exception.DomainException;

/**
 * Value Object representing a user of the ReleasePilot system, capturing their
 * identity and whether they hold approver authority for Promotions.
 *
 * <p>Ensures that the wrapped identifier is never {@code null} or blank.</p>
 *
 * @param id         the underlying user identifier value
 * @param isApprover whether this user is authorized to approve Promotions
 */
public record User(String id, boolean isApprover) {

    public User {
        if (id == null || id.isBlank()) {
            throw new DomainException("User id must not be null or blank");
        }
    }
}
