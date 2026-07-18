package com.releasepilot.query.model;

import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.PromotionStatus;

import java.time.Instant;
import java.util.List;

/**
 * Read model representing the full detail of a single Promotion, including
 * its complete state history.
 *
 * <p>This view is optimized for the {@code GET /promotions/:id} endpoint and is
 * built and maintained exclusively by the read-side projector; it must never be
 * constructed from, or coupled to, the {@code Promotion} write-side aggregate.</p>
 *
 * @param promotionId       the identifier of the promotion
 * @param applicationId     the identifier of the application being promoted
 * @param version           the version of the application being promoted
 * @param sourceEnvironment the source environment of the promotion
 * @param targetEnvironment the target environment of the promotion
 * @param status            the current status of the promotion
 * @param requestedBy       the identifier of the user who requested the promotion
 * @param requestedAt       the instant at which the promotion was requested
 * @param history           the ordered list of state transitions this promotion has gone through
 */
public record PromotionView(
        String promotionId,
        String applicationId,
        String version,
        Environment sourceEnvironment,
        Environment targetEnvironment,
        PromotionStatus status,
        String requestedBy,
        Instant requestedAt,
        List<PromotionHistoryEntry> history
) {
}
