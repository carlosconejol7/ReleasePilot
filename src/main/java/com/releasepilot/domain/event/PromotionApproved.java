package com.releasepilot.domain.event;

import com.releasepilot.domain.model.User;

import java.time.Instant;

/**
 * Domain event signaling that a Promotion has been approved.
 *
 * @param promotionId the identifier of the promotion
 * @param actor       the user who approved the promotion
 * @param occurredAt  the instant at which the event occurred
 */
public record PromotionApproved(
        String promotionId,
        User actor,
        Instant occurredAt
) implements PromotionEvent {
}
