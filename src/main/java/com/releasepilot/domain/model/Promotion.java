package com.releasepilot.domain.model;

import com.releasepilot.domain.exception.DomainException;

import java.util.UUID;

/**
 * Aggregate root representing a Promotion of an Application version
 * from a source {@link Environment} to a target {@link Environment}.
 *
 * <p>Enforces domain invariants regarding valid environment transitions
 * and the allowed status lifecycle transitions.</p>
 */
public class Promotion {

    private final PromotionId id;
    private final ApplicationId applicationId;
    private final Version version;
    private final Environment sourceEnvironment;
    private final Environment targetEnvironment;

    private PromotionStatus status;

    private Promotion(PromotionId id,
                       ApplicationId applicationId,
                       Version version,
                       Environment sourceEnvironment,
                       Environment targetEnvironment,
                       PromotionStatus status) {
        this.id = id;
        this.applicationId = applicationId;
        this.version = version;
        this.sourceEnvironment = sourceEnvironment;
        this.targetEnvironment = targetEnvironment;
        this.status = status;
    }

    /**
     * Requests a new Promotion from the given source Environment to the given target Environment.
     *
     * <p>The transition from source to target must be a valid, forward, single-step
     * environment transition as defined by {@link Environment#canTransitionTo(Environment)}.</p>
     *
     * @param applicationId the identifier of the application being promoted
     * @param version       the version of the application being promoted
     * @param source        the source environment
     * @param target        the target environment
     * @return a new {@link Promotion} instance in {@link PromotionStatus#REQUESTED} status
     * @throws DomainException if the requested environment transition is not allowed
     */
    public static Promotion request(ApplicationId applicationId, Version version, Environment source, Environment target) {
        if (!source.canTransitionTo(target)) {
            throw new DomainException("Cannot promote directly from " + source + " to " + target);
        }

        PromotionId id = new PromotionId(UUID.randomUUID().toString());
        return new Promotion(id, applicationId, version, source, target, PromotionStatus.REQUESTED);
    }

    /**
     * Approves this Promotion.
     *
     * @param approver the identifier of the approver
     * @throws DomainException if the current status cannot transition to APPROVED
     */
    public void approve(String approver) {
        if (!status.canTransitionTo(PromotionStatus.APPROVED)) {
            throw new DomainException("Cannot approve promotion. Current status is " + status);
        }
        this.status = PromotionStatus.APPROVED;
    }

    /**
     * Cancels this Promotion.
     *
     * @param reason the reason for cancellation
     * @throws DomainException if the current status cannot transition to CANCELLED
     */
    public void cancel(String reason) {
        if (!status.canTransitionTo(PromotionStatus.CANCELLED)) {
            throw new DomainException("Cannot cancel promotion. Current status is " + status);
        }
        this.status = PromotionStatus.CANCELLED;
    }

    /**
     * Starts the deployment process for this Promotion.
     *
     * @param operator the identifier of the operator starting the deployment
     * @throws DomainException if the current status cannot transition to DEPLOYMENT_STARTED
     */
    public void startDeployment(String operator) {
        if (!status.canTransitionTo(PromotionStatus.DEPLOYMENT_STARTED)) {
            throw new DomainException("Cannot start deployment. Current status is " + status);
        }
        this.status = PromotionStatus.DEPLOYMENT_STARTED;
    }

    /**
     * Completes the deployment process for this Promotion.
     *
     * @param operator the identifier of the operator completing the deployment
     * @throws DomainException if the current status cannot transition to COMPLETED
     */
    public void completeDeployment(String operator) {
        if (!status.canTransitionTo(PromotionStatus.COMPLETED)) {
            throw new DomainException("Cannot complete deployment. Current status is " + status);
        }
        this.status = PromotionStatus.COMPLETED;
    }

    public PromotionStatus getStatus() {
        return status;
    }
}
