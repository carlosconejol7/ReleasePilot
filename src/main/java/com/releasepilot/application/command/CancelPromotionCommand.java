package com.releasepilot.application.command;

/**
 * Command requesting the cancellation of an existing Promotion.
 *
 * @param promotionId the identifier of the promotion to cancel
 * @param cancelledBy the identifier of the user cancelling the promotion
 * @param reason      the reason for cancellation
 */
public record CancelPromotionCommand(
        String promotionId,
        String cancelledBy,
        String reason
) {
}
