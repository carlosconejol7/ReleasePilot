package com.releasepilot.application.command;

import com.releasepilot.domain.model.User;

/**
 * Command requesting the completion of deployment for an existing Promotion.
 *
 * @param promotionId the identifier of the promotion whose deployment is completing
 * @param operator    the user completing the deployment
 */
public record CompletePromotionCommand(
        String promotionId,
        User operator
) {
}
