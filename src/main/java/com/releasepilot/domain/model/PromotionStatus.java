package com.releasepilot.domain.model;

/**
 * Enum representing the possible statuses of a Promotion throughout its lifecycle.
 */
public enum PromotionStatus {
    REQUESTED,
    APPROVED,
    DEPLOYMENT_STARTED,
    COMPLETED,
    CANCELLED
}
