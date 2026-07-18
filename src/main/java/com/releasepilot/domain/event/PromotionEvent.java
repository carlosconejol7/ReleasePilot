package com.releasepilot.domain.event;

import com.releasepilot.domain.model.User;

import java.time.Instant;

/**
 * Sealed hierarchy representing all domain events emitted by the Promotion aggregate.
 *
 * <p>Every Promotion event carries the identifier of the promotion it relates to,
 * the {@link User} who acted to cause the event, and the instant at which the
 * event occurred.</p>
 */
public sealed interface PromotionEvent
        permits PromotionRequested, PromotionApproved, PromotionStarted, PromotionCancelled, PromotionCompleted, PromotionRolledBack {

    /**
     * @return the identifier of the promotion this event relates to
     */
    String promotionId();

    /**
     * @return the instant at which this event occurred
     */
    Instant occurredAt();

    /**
     * @return the user who performed the action that caused this event
     */
    User actor();
}
