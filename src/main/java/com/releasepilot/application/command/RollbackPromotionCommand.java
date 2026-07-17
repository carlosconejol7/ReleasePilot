package com.releasepilot.application.command;

/**
 * Command requesting the rollback of an existing Promotion.
 *
 * @param promotionId the identifier of the promotion to roll back
 * @param operator    the identifier of the operator performing the rollback
 * @param reason      the reason for the rollback
 */
public record RollbackPromotionCommand(
        String promotionId,
        String operator,
        String reason
) {
}
