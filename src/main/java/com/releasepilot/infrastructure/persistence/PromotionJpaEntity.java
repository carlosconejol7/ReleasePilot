package com.releasepilot.infrastructure.persistence;

import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.PromotionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity mirroring the persisted state of a {@code Promotion} aggregate.
 *
 * <p>This entity exists purely as an infrastructure-layer mapping target for
 * {@link PromotionJpaRepository} / {@link JpaPromotionRepository}. The domain
 * {@code Promotion} aggregate itself remains entirely free of persistence
 * annotations; translation between the two happens exclusively in
 * {@link PromotionEntityMapper}.</p>
 */
@Entity
@Table(name = "promotions")
public class PromotionJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "application_id", nullable = false)
    private String applicationId;

    @Column(name = "version", nullable = false)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_environment", nullable = false)
    private Environment sourceEnvironment;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_environment", nullable = false)
    private Environment targetEnvironment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PromotionStatus status;

    @Column(name = "requested_by_id", nullable = false)
    private String requestedById;

    @Column(name = "requested_by_is_approver", nullable = false)
    private boolean requestedByIsApprover;

    /**
     * Required by JPA.
     */
    protected PromotionJpaEntity() {
    }

    public PromotionJpaEntity(String id,
                               String applicationId,
                               String version,
                               Environment sourceEnvironment,
                               Environment targetEnvironment,
                               PromotionStatus status,
                               String requestedById,
                               boolean requestedByIsApprover) {
        this.id = id;
        this.applicationId = applicationId;
        this.version = version;
        this.sourceEnvironment = sourceEnvironment;
        this.targetEnvironment = targetEnvironment;
        this.status = status;
        this.requestedById = requestedById;
        this.requestedByIsApprover = requestedByIsApprover;
    }

    public String getId() {
        return id;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getVersion() {
        return version;
    }

    public Environment getSourceEnvironment() {
        return sourceEnvironment;
    }

    public Environment getTargetEnvironment() {
        return targetEnvironment;
    }

    public PromotionStatus getStatus() {
        return status;
    }

    public String getRequestedById() {
        return requestedById;
    }

    public boolean isRequestedByIsApprover() {
        return requestedByIsApprover;
    }
}
