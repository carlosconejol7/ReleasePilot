package com.releasepilot.domain.event;

import com.releasepilot.domain.model.User;

import java.time.Instant;

/**
 * Domain event signaling that deployment has started for a Promotion.
 *
 * @param promotionId the identifier of the promotion
 * @param actor       the user who started the deployment
 * @param occurredAt  the instant at which the event occurred
 */
public record PromotionStarted(
        String promotionId,
        User actor,
        Instant occurredAt
) implements PromotionEvent {
}
