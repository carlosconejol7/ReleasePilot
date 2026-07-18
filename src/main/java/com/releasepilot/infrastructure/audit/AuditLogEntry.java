package com.releasepilot.infrastructure.audit;

import java.time.Instant;

/**
 * Immutable record of a single audited Promotion domain event.
 *
 * @param eventType   the simple class name of the domain event that was audited
 * @param promotionId the identifier of the promotion the event relates to
 * @param timestamp   the instant at which the event occurred
 * @param actingUser  the identifier of the user who caused the event
 */
public record AuditLogEntry(
        String eventType,
        String promotionId,
        Instant timestamp,
        String actingUser
) {
}
