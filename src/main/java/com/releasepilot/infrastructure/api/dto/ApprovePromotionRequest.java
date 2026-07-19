package com.releasepilot.infrastructure.api.dto;

import com.releasepilot.domain.model.User;

/**
 * Request body for {@code POST /promotions/{id}/approve}.
 *
 * @param approver the user approving the promotion
 */
public record ApprovePromotionRequest(User approver) {
}
