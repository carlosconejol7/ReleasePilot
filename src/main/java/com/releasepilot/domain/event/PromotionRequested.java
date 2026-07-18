package com.releasepilot.domain.event;

import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.User;

import java.time.Instant;

/**
 * Domain event signaling that a new Promotion has been requested.
 *
 * @param promotionId       the identifier of the promotion
 * @param applicationId     the identifier of the application being promoted
 * @param version           the version of the application being promoted
 * @param sourceEnvironment the source environment of the promotion
 * @param targetEnvironment the target environment of the promotion
 * @param actor             the user who requested the promotion
 * @param occurredAt        the instant at which the event occurred
 */
public record PromotionRequested(
        String promotionId,
        String applicationId,
        String version,
        Environment sourceEnvironment,
        Environment targetEnvironment,
        User actor,
        Instant occurredAt
) implements PromotionEvent {
}
