package com.releasepilot.domain.event;

import com.releasepilot.domain.model.User;

import java.time.Instant;

/**
 * Domain event signaling that a Promotion has been cancelled.
 *
 * @param promotionId the identifier of the promotion
 * @param actor       the user who cancelled the promotion
 * @param reason      the reason for cancellation
 * @param occurredAt  the instant at which the event occurred
 */
public record PromotionCancelled(
        String promotionId,
        User actor,
        String reason,
        Instant occurredAt
) implements PromotionEvent {
}
