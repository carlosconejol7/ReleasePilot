package com.releasepilot.domain.model;

import com.releasepilot.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link Promotion} aggregate, enforcing domain invariants
 * related to environment transitions and status lifecycle rules.
 */
class PromotionTest {

    @Test
    void should_ThrowDomainException_When_EnvironmentIsSkipped() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Environment sourceEnvironment = Environment.DEV;
        Environment targetEnvironment = Environment.PRODUCTION;

        // When / Then
        assertThrows(DomainException.class, () ->
                Promotion.request(applicationId, version, sourceEnvironment, targetEnvironment)
        );
    }

    @Test
    void should_TransitionToApproved_When_ApprovePromotionIsCalledByValidApprover() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Promotion promotion = Promotion.request(applicationId, version, Environment.DEV, Environment.STAGING);

        // When
        promotion.approve("valid-approver");

        // Then
        assertEquals(PromotionStatus.APPROVED, promotion.getStatus());
    }

    @Test
    void should_ThrowDomainException_When_ModifyingTerminalStatus() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Promotion promotion = Promotion.request(applicationId, version, Environment.DEV, Environment.STAGING);
        promotion.cancel("no longer needed");

        // When / Then
        assertThrows(DomainException.class, () -> promotion.approve("valid-approver"));
    }

    @Test
    void should_TransitionToDeploymentStarted_When_StartDeploymentIsCalledOnApprovedPromotion() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Promotion promotion = Promotion.request(applicationId, version, Environment.DEV, Environment.STAGING);
        promotion.approve("valid-approver");

        // When
        promotion.startDeployment("system-operator");

        // Then
        assertEquals(PromotionStatus.DEPLOYMENT_STARTED, promotion.getStatus());
    }

    @Test
    void should_ThrowDomainException_When_StartDeploymentIsCalledOnNonApprovedPromotion() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Promotion promotion = Promotion.request(applicationId, version, Environment.DEV, Environment.STAGING);

        // When / Then
        assertThrows(DomainException.class, () -> promotion.startDeployment("system-operator"));
    }

    @Test
    void should_TransitionToCompleted_When_CompleteDeploymentIsCalledOnStartedPromotion() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Promotion promotion = Promotion.request(applicationId, version, Environment.DEV, Environment.STAGING);
        promotion.approve("valid-approver");
        promotion.startDeployment("system-operator");

        // When
        promotion.completeDeployment("system-operator");

        // Then
        assertEquals(PromotionStatus.COMPLETED, promotion.getStatus());
    }
}
