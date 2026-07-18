package com.releasepilot.query.model;

import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.PromotionStatus;

import java.time.Instant;

/**
 * Lightweight read model representing a single Promotion within a paged history
 * listing for an application.
 *
 * <p>This view is optimized for the {@code GET /applications/:id/promotions} endpoint
 * and intentionally omits the full state history to keep paged listings lean.</p>
 *
 * @param promotionId       the identifier of the promotion
 * @param version           the version of the application being promoted
 * @param sourceEnvironment the source environment of the promotion
 * @param targetEnvironment the target environment of the promotion
 * @param status            the current status of the promotion
 * @param requestedAt       the instant at which the promotion was requested
 * @param lastUpdatedAt     the instant at which the promotion was last updated
 */
public record PromotionSummaryView(
        String promotionId,
        String version,
        Environment sourceEnvironment,
        Environment targetEnvironment,
        PromotionStatus status,
        Instant requestedAt,
        Instant lastUpdatedAt
) {
}
