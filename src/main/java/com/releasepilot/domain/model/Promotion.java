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
    private final String requestedBy;

    private PromotionStatus status;

    private Promotion(PromotionId id,
                       ApplicationId applicationId,
                       Version version,
                       Environment sourceEnvironment,
                       Environment targetEnvironment,
                       String requestedBy,
                       PromotionStatus status) {
        this.id = id;
        this.applicationId = applicationId;
        this.version = version;
        this.sourceEnvironment = sourceEnvironment;
        this.targetEnvironment = targetEnvironment;
        this.requestedBy = requestedBy;
        this.status = status;
    }

    /**
     * Requests a new Promotion from the given source Environment to the given target Environment.
     *
     * <p>The transition from source to target must be a valid, forward, single-step
     * environment transition as defined by {@link Environment#canTransitionTo(Environment)}.</p>
     *
     * <p>Additionally, the caller must supply the results of the following history checks,
     * which are enforced as aggregate invariants:</p>
     * <ul>
     *     <li>{@code hasActivePromotionInTarget} - whether another promotion for the same
     *     application and target environment is already in progress. Only one such promotion
     *     may be active at a time.</li>
     *     <li>{@code hasCompletedPreviousEnvironment} - whether the version being promoted has
     *     already completed a promotion into the source environment.</li>
     * </ul>
     *
     * @param applicationId                   the identifier of the application being promoted
     * @param version                          the version of the application being promoted
     * @param source                           the source environment
     * @param target                           the target environment
     * @param requestedBy                      the identifier of the user requesting the promotion
     * @param hasCompletedPreviousEnvironment  whether the version has completed the source environment
     * @param hasActivePromotionInTarget       whether an active promotion already exists for the target environment
     * @return a new {@link Promotion} instance in {@link PromotionStatus#REQUESTED} status
     * @throws DomainException if the requested environment transition is not allowed,
     *                         if another promotion is already active in the target environment,
     *                         or if the version has not completed the source environment
     */
    public static Promotion request(ApplicationId applicationId,
                                     Version version,
                                     Environment source,
                                     Environment target,
                                     String requestedBy,
                                     boolean hasCompletedPreviousEnvironment,
                                     boolean hasActivePromotionInTarget) {
        if (!source.canTransitionTo(target)) {
            throw new DomainException("Cannot promote directly from " + source + " to " + target);
        }

        if (hasActivePromotionInTarget) {
            throw new DomainException("Only one promotion may be in progress per application + target environment");
        }

        if (!hasCompletedPreviousEnvironment) {
            throw new DomainException(version.value() + " has not completed " + source);
        }

        PromotionId id = new PromotionId(UUID.randomUUID().toString());
        return new Promotion(id, applicationId, version, source, target, requestedBy, PromotionStatus.REQUESTED);
    }

    /**
     * Approves this Promotion.
     *
     * @param approver the approver requesting to approve this promotion
     * @throws DomainException if the current status cannot transition to APPROVED,
     *                         or if the approver is the same user who requested the promotion
     */
    public void approve(Approver approver) {
        if (!status.canTransitionTo(PromotionStatus.APPROVED)) {
            throw new DomainException("Cannot approve promotion. Current status is " + status);
        }
        if (this.requestedBy.equalsIgnoreCase(approver.value())) {
            throw new DomainException("The requester cannot approve their own promotion request.");
        }
        this.status = PromotionStatus.APPROVED;
    }

    /**
     * Cancels this Promotion.
     *
     * @param cancelledBy the identifier of the user cancelling the promotion
     * @param reason      the reason for cancellation
     * @throws DomainException if the current status cannot transition to CANCELLED
     */
    public void cancel(String cancelledBy, String reason) {
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

    /**
     * Rolls back this Promotion, either due to a failed deployment or as a post-deployment reversal.
     *
     * @param operator the identifier of the operator performing the rollback
     * @param reason   the reason for the rollback
     * @throws DomainException if the current status cannot transition to ROLLED_BACK
     */
    public void rollback(String operator, String reason) {
        if (!status.canTransitionTo(PromotionStatus.ROLLED_BACK)) {
            throw new DomainException("Cannot rollback promotion. Current status is " + status);
        }
        this.status = PromotionStatus.ROLLED_BACK;
    }

    public PromotionId getId() {
        return id;
    }

    public PromotionStatus getStatus() {
        return status;
    }
}
