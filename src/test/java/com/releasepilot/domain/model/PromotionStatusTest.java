package com.releasepilot.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link PromotionStatus} enum, enforcing valid status
 * lifecycle transition rules for a Promotion.
 */
class PromotionStatusTest {

    @Test
    void should_AllowTransitionToApproved_When_CurrentStatusIsRequested() {
        assertTrue(PromotionStatus.REQUESTED.canTransitionTo(PromotionStatus.APPROVED));
    }

    @Test
    void should_AllowTransitionToCancelled_When_CurrentStatusIsRequested() {
        assertTrue(PromotionStatus.REQUESTED.canTransitionTo(PromotionStatus.CANCELLED));
    }

    @Test
    void should_DenyTransitionToDeploymentStarted_When_CurrentStatusIsRequested() {
        assertFalse(PromotionStatus.REQUESTED.canTransitionTo(PromotionStatus.DEPLOYMENT_STARTED));
    }

    @Test
    void should_DenyTransitionToCompleted_When_CurrentStatusIsRequested() {
        assertFalse(PromotionStatus.REQUESTED.canTransitionTo(PromotionStatus.COMPLETED));
    }

    @Test
    void should_DenyTransitionToRequested_When_CurrentStatusIsRequested() {
        assertFalse(PromotionStatus.REQUESTED.canTransitionTo(PromotionStatus.REQUESTED));
    }

    @Test
    void should_AllowTransitionToDeploymentStarted_When_CurrentStatusIsApproved() {
        assertTrue(PromotionStatus.APPROVED.canTransitionTo(PromotionStatus.DEPLOYMENT_STARTED));
    }

    @Test
    void should_AllowTransitionToCancelled_When_CurrentStatusIsApproved() {
        assertTrue(PromotionStatus.APPROVED.canTransitionTo(PromotionStatus.CANCELLED));
    }

    @Test
    void should_DenyTransitionToRequested_When_CurrentStatusIsApproved() {
        assertFalse(PromotionStatus.APPROVED.canTransitionTo(PromotionStatus.REQUESTED));
    }

    @Test
    void should_DenyTransitionToCompleted_When_CurrentStatusIsApproved() {
        assertFalse(PromotionStatus.APPROVED.canTransitionTo(PromotionStatus.COMPLETED));
    }

    @Test
    void should_DenyTransitionToApproved_When_CurrentStatusIsApproved() {
        assertFalse(PromotionStatus.APPROVED.canTransitionTo(PromotionStatus.APPROVED));
    }

    @Test
    void should_AllowTransitionToCompleted_When_CurrentStatusIsDeploymentStarted() {
        assertTrue(PromotionStatus.DEPLOYMENT_STARTED.canTransitionTo(PromotionStatus.COMPLETED));
    }

    @Test
    void should_DenyTransitionToRequested_When_CurrentStatusIsDeploymentStarted() {
        assertFalse(PromotionStatus.DEPLOYMENT_STARTED.canTransitionTo(PromotionStatus.REQUESTED));
    }

    @Test
    void should_DenyTransitionToApproved_When_CurrentStatusIsDeploymentStarted() {
        assertFalse(PromotionStatus.DEPLOYMENT_STARTED.canTransitionTo(PromotionStatus.APPROVED));
    }

    @Test
    void should_AllowTransitionToCancelled_When_CurrentStatusIsDeploymentStarted() {
        assertTrue(PromotionStatus.DEPLOYMENT_STARTED.canTransitionTo(PromotionStatus.CANCELLED));
    }

    @Test
    void should_DenyTransitionToDeploymentStarted_When_CurrentStatusIsDeploymentStarted() {
        assertFalse(PromotionStatus.DEPLOYMENT_STARTED.canTransitionTo(PromotionStatus.DEPLOYMENT_STARTED));
    }

    @Test
    void should_DenyAnyTransition_When_CurrentStatusIsCompleted() {
        for (PromotionStatus target : PromotionStatus.values()) {
            assertFalse(PromotionStatus.COMPLETED.canTransitionTo(target));
        }
    }

    @Test
    void should_DenyAnyTransition_When_CurrentStatusIsCancelled() {
        for (PromotionStatus target : PromotionStatus.values()) {
            assertFalse(PromotionStatus.CANCELLED.canTransitionTo(target));
        }
    }
}
