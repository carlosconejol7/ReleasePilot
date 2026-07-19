package com.releasepilot.infrastructure.api.dto;

import com.releasepilot.domain.model.User;

/**
 * Request body for {@code POST /promotions/{id}/start}.
 *
 * @param operator the user starting the deployment
 */
public record StartDeploymentRequest(User operator) {
}
