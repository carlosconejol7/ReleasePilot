package com.releasepilot.domain.event;

import java.time.Instant;

/**
 * Domain event signaling that deployment has started for a Promotion.
 *
 * @param promotionId the identifier of the promotion
 * @param actingUser  the identifier of the operator who started the deployment
 * @param occurredAt  the instant at which the event occurred
 */
public record DeploymentStarted(
        String promotionId,
        String actingUser,
        Instant occurredAt
) {
}
