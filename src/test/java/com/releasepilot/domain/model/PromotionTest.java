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

    private static final User REQUESTER = new User("user-1", false);
    private static final User VALID_APPROVER = new User("user-2", true);
    private static final User OPERATOR = new User("system-operator", true);

    @Test
    void should_ThrowDomainException_When_EnvironmentIsSkipped() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Environment sourceEnvironment = Environment.DEV;
        Environment targetEnvironment = Environment.PRODUCTION;

        // When / Then
        assertThrows(DomainException.class, () ->
                Promotion.request(applicationId, version, sourceEnvironment, targetEnvironment, REQUESTER, true, false)
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
                Promotion.request(applicationId, version, sourceEnvironment, targetEnvironment, REQUESTER, true, false)
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
                Promotion.request(applicationId, version, sourceEnvironment, targetEnvironment, REQUESTER, true, false)
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
                Promotion.request(applicationId, version, sourceEnvironment, targetEnvironment, REQUESTER, false, false)
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
                Promotion.request(applicationId, version, sourceEnvironment, targetEnvironment, REQUESTER, true, true)
        );
        assertEquals("Only one promotion may be in progress per application + target environment", exception.getMessage());
    }

    @Test
    void should_TransitionToApproved_When_ApprovePromotionIsCalledByValidApprover() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Promotion promotion = Promotion.request(applicationId, version, Environment.DEV, Environment.STAGING, REQUESTER, true, false);

        // When
        promotion.approve(VALID_APPROVER);

        // Then
        assertEquals(PromotionStatus.APPROVED, promotion.getStatus());
    }

    @Test
    void should_ThrowDomainException_When_NonApproverAttemptsToApprove() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Promotion promotion = Promotion.request(applicationId, version, Environment.DEV, Environment.STAGING, REQUESTER, true, false);
        User nonApprover = new User("user-3", false);

        // When / Then
        DomainException exception = assertThrows(DomainException.class, () -> promotion.approve(nonApprover));
        assertEquals("User is not authorized to approve promotions", exception.getMessage());
    }

    @Test
    void should_ThrowDomainException_When_RequesterTriesToApproveTheirOwnPromotion() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Promotion promotion = Promotion.request(applicationId, version, Environment.DEV, Environment.STAGING, REQUESTER, true, false);
        User requesterAsApprover = new User(REQUESTER.id(), true);

        // When / Then
        DomainException exception = assertThrows(DomainException.class, () -> promotion.approve(requesterAsApprover));
        assertEquals("The requester cannot approve their own promotion request.", exception.getMessage());
    }

    @Test
    void should_ThrowDomainException_When_ModifyingTerminalStatus() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Promotion promotion = Promotion.request(applicationId, version, Environment.DEV, Environment.STAGING, REQUESTER, true, false);
        promotion.cancel(REQUESTER, "no longer needed");

        // When / Then
        assertThrows(DomainException.class, () -> promotion.approve(VALID_APPROVER));
    }

    @Test
    void should_TransitionToCancelled_When_CancelIsCalledOnDeploymentStartedPromotion() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Promotion promotion = Promotion.request(applicationId, version, Environment.DEV, Environment.STAGING, REQUESTER, true, false);
        promotion.approve(VALID_APPROVER);
        promotion.startDeployment(OPERATOR);

        // When
        promotion.cancel(REQUESTER, "aborting deployment");

        // Then
        assertEquals(PromotionStatus.CANCELLED, promotion.getStatus());
    }

    @Test
    void should_TransitionToRolledBack_When_RollbackIsCalledOnDeploymentStartedPromotion() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Promotion promotion = Promotion.request(applicationId, version, Environment.DEV, Environment.STAGING, REQUESTER, true, false);
        promotion.approve(VALID_APPROVER);
        promotion.startDeployment(OPERATOR);

        // When
        promotion.rollback(OPERATOR, "deployment failed");

        // Then
        assertEquals(PromotionStatus.ROLLED_BACK, promotion.getStatus());
    }

    @Test
    void should_ThrowDomainException_When_RollbackIsCalledOnRequestedPromotion() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Promotion promotion = Promotion.request(applicationId, version, Environment.DEV, Environment.STAGING, REQUESTER, true, false);

        // When / Then
        DomainException exception = assertThrows(DomainException.class,
                () -> promotion.rollback(OPERATOR, "not deployed yet"));
        assertEquals("Cannot rollback promotion. Current status is REQUESTED", exception.getMessage());
    }

    @Test
    void should_ThrowDomainException_When_RollbackIsCalledOnCompletedPromotion() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Promotion promotion = Promotion.request(applicationId, version, Environment.DEV, Environment.STAGING, REQUESTER, true, false);
        promotion.approve(VALID_APPROVER);
        promotion.startDeployment(OPERATOR);
        promotion.completeDeployment(OPERATOR);

        // When / Then
        DomainException exception = assertThrows(DomainException.class,
                () -> promotion.rollback(OPERATOR, "post-deployment issue detected"));
        assertEquals("Cannot rollback promotion. Current status is COMPLETED", exception.getMessage());
    }

    @Test
    void should_TransitionToDeploymentStarted_When_StartDeploymentIsCalledOnApprovedPromotion() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Promotion promotion = Promotion.request(applicationId, version, Environment.DEV, Environment.STAGING, REQUESTER, true, false);
        promotion.approve(VALID_APPROVER);

        // When
        promotion.startDeployment(OPERATOR);

        // Then
        assertEquals(PromotionStatus.DEPLOYMENT_STARTED, promotion.getStatus());
    }

    @Test
    void should_ThrowDomainException_When_StartDeploymentIsCalledOnNonApprovedPromotion() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Promotion promotion = Promotion.request(applicationId, version, Environment.DEV, Environment.STAGING, REQUESTER, true, false);

        // When / Then
        assertThrows(DomainException.class, () -> promotion.startDeployment(OPERATOR));
    }

    @Test
    void should_TransitionToCompleted_When_CompleteDeploymentIsCalledOnStartedPromotion() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Promotion promotion = Promotion.request(applicationId, version, Environment.DEV, Environment.STAGING, REQUESTER, true, false);
        promotion.approve(VALID_APPROVER);
        promotion.startDeployment(OPERATOR);

        // When
        promotion.completeDeployment(OPERATOR);

        // Then
        assertEquals(PromotionStatus.COMPLETED, promotion.getStatus());
    }

    @Test
    void should_ThrowDomainException_When_StartDeploymentIsCalledOnCompletedPromotion() {
        // Given
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        Promotion promotion = Promotion.request(applicationId, version, Environment.DEV, Environment.STAGING, REQUESTER, true, false);
        promotion.approve(VALID_APPROVER);
        promotion.startDeployment(OPERATOR);
        promotion.completeDeployment(OPERATOR);

        // When / Then
        DomainException exception = assertThrows(DomainException.class,
                () -> promotion.startDeployment(OPERATOR));
        assertEquals("Cannot start deployment. Current status is COMPLETED", exception.getMessage());
    }
}
