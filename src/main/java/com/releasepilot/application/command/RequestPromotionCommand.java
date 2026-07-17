package com.releasepilot.application.command;

import com.releasepilot.domain.model.Environment;

/**
 * Command requesting a new Promotion for a given application version
 * from a source Environment to a target Environment.
 *
 * @param applicationId     the identifier of the application being promoted
 * @param version           the version of the application being promoted
 * @param sourceEnvironment the source environment
 * @param targetEnvironment the target environment
 */
public record RequestPromotionCommand(
        String applicationId,
        String version,
        Environment sourceEnvironment,
        Environment targetEnvironment
) {
}
