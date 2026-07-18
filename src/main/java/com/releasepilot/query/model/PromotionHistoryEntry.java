package com.releasepilot.query.model;

import com.releasepilot.domain.model.PromotionStatus;

import java.time.Instant;

/**
 * Read model representing a single state transition in a Promotion's history.
 *
 * @param status     the status the promotion transitioned into
 * @param actor      the identifier of the user who caused this transition
 * @param occurredAt the instant at which this transition occurred
 * @param reason     the reason for this transition, if applicable (e.g. cancellation
 *                   or rollback reason); {@code null} otherwise
 */
public record PromotionHistoryEntry(
        PromotionStatus status,
        String actor,
        Instant occurredAt,
        String reason
) {
}
