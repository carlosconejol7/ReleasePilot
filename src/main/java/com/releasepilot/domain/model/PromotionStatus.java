package com.releasepilot.domain.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Enum representing the possible statuses of a Promotion throughout its lifecycle.
 *
 * <p>Allowed transitions between statuses are declared in a static lookup table
 * rather than through conditional logic, keeping the transition rules explicit
 * and easy to maintain.</p>
 */
public enum PromotionStatus {
    REQUESTED,
    APPROVED,
    DEPLOYMENT_STARTED,
    COMPLETED,
    CANCELLED,
    ROLLED_BACK;

    private static final Map<PromotionStatus, Set<PromotionStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(PromotionStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(REQUESTED, EnumSet.of(APPROVED, CANCELLED));
        ALLOWED_TRANSITIONS.put(APPROVED, EnumSet.of(DEPLOYMENT_STARTED, CANCELLED));
        ALLOWED_TRANSITIONS.put(DEPLOYMENT_STARTED, EnumSet.of(COMPLETED, CANCELLED, ROLLED_BACK));
        ALLOWED_TRANSITIONS.put(COMPLETED, EnumSet.of(ROLLED_BACK));
        ALLOWED_TRANSITIONS.put(CANCELLED, EnumSet.noneOf(PromotionStatus.class));
        ALLOWED_TRANSITIONS.put(ROLLED_BACK, EnumSet.noneOf(PromotionStatus.class));
    }

    /**
     * Determines whether a transition from this status to the given target status is allowed.
     *
     * @param target the target status
     * @return {@code true} if the transition is declared as allowed, {@code false} otherwise
     */
    public boolean canTransitionTo(PromotionStatus target) {
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }
}
