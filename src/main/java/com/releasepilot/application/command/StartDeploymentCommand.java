package com.releasepilot.application.command;

import com.releasepilot.domain.model.User;

/**
 * Command requesting the start of deployment for an existing Promotion.
 *
 * @param promotionId the identifier of the promotion whose deployment is starting
 * @param operator    the user starting the deployment
 */
public record StartDeploymentCommand(
        String promotionId,
        User operator
) {
}
