package com.releasepilot.application.command;

import com.releasepilot.domain.model.User;

/**
 * Command requesting the rollback of an existing Promotion.
 *
 * @param promotionId the identifier of the promotion to roll back
 * @param operator    the user performing the rollback
 * @param reason      the reason for the rollback
 */
public record RollbackPromotionCommand(
        String promotionId,
        User operator,
        String reason
) {
}
