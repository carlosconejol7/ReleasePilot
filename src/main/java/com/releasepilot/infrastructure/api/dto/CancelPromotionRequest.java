package com.releasepilot.infrastructure.api.dto;

import com.releasepilot.domain.model.User;

/**
 * Request body for {@code POST /promotions/{id}/cancel}.
 *
 * @param cancelledBy the user cancelling the promotion
 * @param reason      the reason for cancellation
 */
public record CancelPromotionRequest(User cancelledBy, String reason) {
}
