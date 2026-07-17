package com.releasepilot.domain.event;

import java.time.Instant;

/**
 * Domain event signaling that a Promotion has been approved.
 *
 * @param promotionId the identifier of the promotion
 * @param actingUser  the identifier of the user who approved the promotion
 * @param occurredAt  the instant at which the event occurred
 */
public record PromotionApproved(
        String promotionId,
        String actingUser,
        Instant occurredAt
) {
}
