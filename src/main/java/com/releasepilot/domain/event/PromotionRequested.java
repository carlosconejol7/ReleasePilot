package com.releasepilot.domain.event;

import java.time.Instant;

/**
 * Domain event signaling that a new Promotion has been requested.
 *
 * @param promotionId   the identifier of the promotion
 * @param applicationId the identifier of the application being promoted
 * @param version       the version of the application being promoted
 * @param actingUser    the identifier of the user who requested the promotion
 * @param occurredAt    the instant at which the event occurred
 */
public record PromotionRequested(
        String promotionId,
        String applicationId,
        String version,
        String actingUser,
        Instant occurredAt
) {
}
