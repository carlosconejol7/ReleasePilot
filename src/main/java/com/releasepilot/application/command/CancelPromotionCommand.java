package com.releasepilot.application.command;

import com.releasepilot.domain.model.User;

/**
 * Command requesting the cancellation of an existing Promotion.
 *
 * @param promotionId the identifier of the promotion to cancel
 * @param cancelledBy the user cancelling the promotion
 * @param reason      the reason for cancellation
 */
public record CancelPromotionCommand(
        String promotionId,
        User cancelledBy,
        String reason
) {
}
