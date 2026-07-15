package com.releasepilot.domain.model;

/**
 * Enum representing the deployment environments supported by ReleasePilot.
 *
 * <p>Each environment has an associated {@code step} that defines its position
 * in the promotion pipeline. A transition is only valid when moving forward
 * exactly one step at a time (e.g. DEV -> STAGING -> PRODUCTION).</p>
 */
public enum Environment {
    DEV(1),
    STAGING(2),
    PRODUCTION(3);

    private final int step;

    Environment(int step) {
        this.step = step;
    }

    /**
     * Determines whether a promotion from this environment to the given target environment
     * is allowed. Only forward transitions of exactly one step are permitted.
     *
     * @param target the target environment
     * @return {@code true} if the target environment's step is exactly one greater than this
     *         environment's step, {@code false} otherwise
     */
    public boolean canTransitionTo(Environment target) {
        return target.step == this.step + 1;
    }
}
