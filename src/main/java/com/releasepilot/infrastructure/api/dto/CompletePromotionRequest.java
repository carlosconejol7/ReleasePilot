package com.releasepilot.infrastructure.api.dto;

import com.releasepilot.domain.model.User;

/**
 * Request body for {@code POST /promotions/{id}/complete}.
 *
 * @param operator the user completing the deployment
 */
public record CompletePromotionRequest(User operator) {
}
