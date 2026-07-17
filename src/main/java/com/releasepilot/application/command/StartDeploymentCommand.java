package com.releasepilot.application.command;

/**
 * Command requesting the start of deployment for an existing Promotion.
 *
 * @param promotionId the identifier of the promotion whose deployment is starting
 * @param operator    the identifier of the operator starting the deployment
 */
public record StartDeploymentCommand(
        String promotionId,
        String operator
) {
}
