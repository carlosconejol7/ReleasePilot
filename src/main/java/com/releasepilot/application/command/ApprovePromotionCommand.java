package com.releasepilot.application.command;

/**
 * Command requesting the approval of an existing Promotion.
 *
 * @param promotionId the identifier of the promotion to approve
 * @param approver    the identifier of the user approving the promotion
 */
public record ApprovePromotionCommand(
        String promotionId,
        String approver
) {
}
