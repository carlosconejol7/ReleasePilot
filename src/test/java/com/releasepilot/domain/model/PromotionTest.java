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
                Promotion.request(applicationId, version, sourceEnvironment, targetEnvironment, "user-1", true, false)
        );
    }

    @Test
    void should_ThrowDomainException_When_PromotingBackwards() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Environment sourceEnvironment = Environment.STAGING;
        Environment targetEnvironment = Environment.DEV;

        // When / Then
        assertThrows(DomainException.class, () ->
                Promotion.request(applicationId, version, sourceEnvironment, targetEnvironment, "user-1", true, false)
        );
    }

    @Test
    void should_ThrowDomainException_When_PromotingToSameEnvironment() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Environment sourceEnvironment = Environment.DEV;
        Environment targetEnvironment = Environment.DEV;

        // When / Then
        assertThrows(DomainException.class, () ->
                Promotion.request(applicationId, version, sourceEnvironment, targetEnvironment, "user-1", true, false)
        );
    }

    @Test
    void should_ThrowDomainException_When_VersionHasNotCompletedPreviousEnvironment() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Environment sourceEnvironment = Environment.DEV;
        Environment targetEnvironment = Environment.STAGING;

        // When / Then
        DomainException exception = assertThrows(DomainException.class, () ->
                Promotion.request(applicationId, version, sourceEnvironment, targetEnvironment, "user-1", false, false)
        );
        assertEquals("1.0.0 has not completed DEV", exception.getMessage());
    }

    @Test
    void should_ThrowDomainException_When_AnotherPromotionIsAlreadyInProgressInTarget() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Environment sourceEnvironment = Environment.DEV;
        Environment targetEnvironment = Environment.STAGING;

        // When / Then
        DomainException exception = assertThrows(DomainException.class, () ->
                Promotion.request(applicationId, version, sourceEnvironment, targetEnvironment, "user-1", true, true)
        );
        assertEquals("Only one promotion may be in progress per application + target environment", exception.getMessage());
    }

    @Test
    void should_TransitionToApproved_When_ApprovePromotionIsCalledByValidApprover() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Promotion promotion = Promotion.request(applicationId, version, Environment.DEV, Environment.STAGING, "user-1", true, false);

        // When
        promotion.approve("user-2");

        // Then
        assertEquals(PromotionStatus.APPROVED, promotion.getStatus());
    }

    @Test
    void should_ThrowDomainException_When_RequesterTriesToApproveTheirOwnPromotion() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Promotion promotion = Promotion.request(applicationId, version, Environment.DEV, Environment.STAGING, "user-1", true, false);

        // When / Then
        DomainException exception = assertThrows(DomainException.class, () -> promotion.approve("user-1"));
        assertEquals("The requester cannot approve their own promotion request.", exception.getMessage());
    }

    @Test
    void should_ThrowDomainException_When_ModifyingTerminalStatus() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Promotion promotion = Promotion.request(applicationId, version, Environment.DEV, Environment.STAGING, "user-1", true, false);
        promotion.cancel("no longer needed");

        // When / Then
        assertThrows(DomainException.class, () -> promotion.approve("user-2"));
    }

    @Test
    void should_TransitionToDeploymentStarted_When_StartDeploymentIsCalledOnApprovedPromotion() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Promotion promotion = Promotion.request(applicationId, version, Environment.DEV, Environment.STAGING, "user-1", true, false);
        promotion.approve("user-2");

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
        Promotion promotion = Promotion.request(applicationId, version, Environment.DEV, Environment.STAGING, "user-1", true, false);

        // When / Then
        assertThrows(DomainException.class, () -> promotion.startDeployment("system-operator"));
    }

    @Test
    void should_TransitionToCompleted_When_CompleteDeploymentIsCalledOnStartedPromotion() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Promotion promotion = Promotion.request(applicationId, version, Environment.DEV, Environment.STAGING, "user-1", true, false);
        promotion.approve("user-2");
        promotion.startDeployment("system-operator");

        // When
        promotion.completeDeployment("system-operator");

        // Then
        assertEquals(PromotionStatus.COMPLETED, promotion.getStatus());
    }

    @Test
    void should_ThrowDomainException_When_StartDeploymentIsCalledOnCompletedPromotion() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Promotion promotion = Promotion.request(applicationId, version, Environment.DEV, Environment.STAGING, "user-1", true, false);
        promotion.approve("user-2");
        promotion.startDeployment("system-operator");
        promotion.completeDeployment("system-operator");

        // When / Then
        DomainException exception = assertThrows(DomainException.class,
                () -> promotion.startDeployment("system-operator"));
        assertEquals("Cannot start deployment. Current status is COMPLETED", exception.getMessage());
    }
}
