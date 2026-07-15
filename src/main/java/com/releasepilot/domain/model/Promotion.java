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
     * <p>Skipping the {@code STAGING} environment when promoting from {@code DEV} directly
     * to {@code PRODUCTION} is not allowed.</p>
     *
     * @param applicationId the identifier of the application being promoted
     * @param version       the version of the application being promoted
     * @param source        the source environment
     * @param target        the target environment
     * @return a new {@link Promotion} instance in {@link PromotionStatus#REQUESTED} status
     * @throws DomainException if the requested transition skips the STAGING environment
     */
    public static Promotion request(ApplicationId applicationId, Version version, Environment source, Environment target) {
        if (source == Environment.DEV && target == Environment.PRODUCTION) {
            throw new DomainException("Cannot promote directly from DEV to PRODUCTION: STAGING environment must not be skipped");
        }

        PromotionId id = new PromotionId(UUID.randomUUID().toString());
        return new Promotion(id, applicationId, version, source, target, PromotionStatus.REQUESTED);
    }

    /**
     * Approves this Promotion.
     *
     * @param approver the identifier of the approver
     * @throws DomainException if the current status is a terminal state (CANCELLED or COMPLETED)
     *                          or if the Promotion is already APPROVED
     */
    public void approve(String approver) {
        if (status == PromotionStatus.CANCELLED || status == PromotionStatus.COMPLETED) {
            throw new DomainException("Cannot approve a Promotion in terminal status: " + status);
        }
        if (status == PromotionStatus.APPROVED) {
            throw new DomainException("Promotion is already approved");
        }
        this.status = PromotionStatus.APPROVED;
    }

    /**
     * Cancels this Promotion.
     *
     * @param reason the reason for cancellation
     * @throws DomainException if the current status is already a terminal state (CANCELLED or COMPLETED)
     */
    public void cancel(String reason) {
        if (status == PromotionStatus.CANCELLED || status == PromotionStatus.COMPLETED) {
            throw new DomainException("Cannot cancel a Promotion already in terminal status: " + status);
        }
        this.status = PromotionStatus.CANCELLED;
    }

    public PromotionStatus getStatus() {
        return status;
    }
}
