package com.releasepilot.infrastructure.api.dto;

/**
 * Response body returned after successfully requesting a new Promotion.
 *
 * @param promotionId the identifier of the newly created promotion
 */
public record PromotionIdResponse(String promotionId) {
}
