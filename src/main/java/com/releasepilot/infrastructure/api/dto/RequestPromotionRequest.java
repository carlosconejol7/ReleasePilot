package com.releasepilot.infrastructure.api.dto;

import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.User;

/**
 * Request body for {@code POST /promotions}.
 *
 * @param applicationId     the identifier of the application being promoted
 * @param version           the version of the application being promoted
 * @param sourceEnvironment the source environment
 * @param targetEnvironment the target environment
 * @param requestedBy       the user requesting the promotion
 */
public record RequestPromotionRequest(
        String applicationId,
        String version,
        Environment sourceEnvironment,
        Environment targetEnvironment,
        User requestedBy
) {
}
