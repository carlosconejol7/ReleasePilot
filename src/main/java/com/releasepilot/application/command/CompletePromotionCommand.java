package com.releasepilot.application.command;

/**
 * Command requesting the completion of deployment for an existing Promotion.
 *
 * @param promotionId the identifier of the promotion whose deployment is completing
 * @param operator    the identifier of the operator completing the deployment
 */
public record CompletePromotionCommand(
        String promotionId,
        String operator
) {
}
