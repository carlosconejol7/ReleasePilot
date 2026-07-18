package com.releasepilot.application.command;

import com.releasepilot.domain.model.User;

/**
 * Command requesting the approval of an existing Promotion.
 *
 * @param promotionId the identifier of the promotion to approve
 * @param approver    the user approving the promotion
 */
public record ApprovePromotionCommand(
        String promotionId,
        User approver
) {
}
