package com.releasepilot.domain.event;

import java.time.Instant;

/**
 * Domain event signaling that a Promotion has been rolled back.
 *
 * @param promotionId the identifier of the promotion
 * @param actingUser  the identifier of the operator who performed the rollback
 * @param reason      the reason for the rollback
 * @param occurredAt  the instant at which the event occurred
 */
public record PromotionRolledBack(
        String promotionId,
        String actingUser,
        String reason,
        Instant occurredAt
) {
}
