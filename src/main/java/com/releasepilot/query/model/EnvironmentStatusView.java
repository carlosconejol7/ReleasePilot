package com.releasepilot.query.model;

import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.PromotionStatus;

import java.time.Instant;

/**
 * Read model representing the current promotion state of a single Environment
 * for a given application.
 *
 * @param environment the environment this status describes
 * @param version     the version currently associated with this environment
 * @param promotionId the identifier of the promotion that last affected this environment
 * @param status      the current status of that promotion
 * @param updatedAt   the instant at which this status was last updated
 */
public record EnvironmentStatusView(
        Environment environment,
        String version,
        String promotionId,
        PromotionStatus status,
        Instant updatedAt
) {
}
