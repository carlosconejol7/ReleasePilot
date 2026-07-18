package com.releasepilot.domain.event;

import com.releasepilot.domain.model.User;

import java.time.Instant;

/**
 * Domain event signaling that a Promotion has been rolled back.
 *
 * @param promotionId the identifier of the promotion
 * @param actor       the user who performed the rollback
 * @param reason      the reason for the rollback
 * @param occurredAt  the instant at which the event occurred
 */
public record PromotionRolledBack(
        String promotionId,
        User actor,
        String reason,
        Instant occurredAt
) implements PromotionEvent {
}
