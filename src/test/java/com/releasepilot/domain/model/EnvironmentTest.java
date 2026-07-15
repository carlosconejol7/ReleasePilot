package com.releasepilot.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link Environment} enum, enforcing valid promotion
 * transition rules between deployment environments.
 */
class EnvironmentTest {

    @Test
    void should_ReturnTrue_When_TransitioningFromDevToStaging() {
        assertTrue(Environment.DEV.canTransitionTo(Environment.STAGING));
    }

    @Test
    void should_ReturnTrue_When_TransitioningFromStagingToProduction() {
        assertTrue(Environment.STAGING.canTransitionTo(Environment.PRODUCTION));
    }

    @Test
    void should_ReturnFalse_When_TransitioningFromDevToProductionSkippingStaging() {
        assertFalse(Environment.DEV.canTransitionTo(Environment.PRODUCTION));
    }

    @Test
    void should_ReturnFalse_When_TransitioningFromStagingBackToDev() {
        assertFalse(Environment.STAGING.canTransitionTo(Environment.DEV));
    }

    @Test
    void should_ReturnFalse_When_TransitioningFromProductionBackToStaging() {
        assertFalse(Environment.PRODUCTION.canTransitionTo(Environment.STAGING));
    }

    @Test
    void should_ReturnFalse_When_TransitioningFromDevToItself() {
        assertFalse(Environment.DEV.canTransitionTo(Environment.DEV));
    }
}
