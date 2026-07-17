package com.releasepilot.domain.event;

import java.time.Instant;

/**
 * Domain event signaling that a Promotion's deployment has completed.
 *
 * @param promotionId the identifier of the promotion
 * @param actingUser  the identifier of the operator who completed the deployment
 * @param occurredAt  the instant at which the event occurred
 */
public record PromotionCompleted(
        String promotionId,
        String actingUser,
        Instant occurredAt
) {
}
