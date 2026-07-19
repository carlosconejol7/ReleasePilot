package com.releasepilot.infrastructure.api.dto;

import com.releasepilot.domain.model.User;

/**
 * Request body for {@code POST /promotions/{id}/rollback}.
 *
 * @param operator the user performing the rollback
 * @param reason   the reason for the rollback
 */
public record RollbackPromotionRequest(User operator, String reason) {
}
