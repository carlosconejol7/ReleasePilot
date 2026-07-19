package com.releasepilot.infrastructure.persistence;

import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.PromotionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

/**
 * Spring Data JPA repository for {@link PromotionJpaEntity}.
 *
 * <p>Exposes the two derived-query methods required to support the multi-promotion
 * history checks declared by the domain's {@code PromotionRepository} port, in addition
 * to the standard {@link JpaRepository} CRUD operations.</p>
 */
public interface PromotionJpaRepository extends JpaRepository<PromotionJpaEntity, String> {

    /**
     * Checks whether a Promotion exists for the given application and target environment
     * whose status is one of the given (non-terminal) statuses.
     */
    boolean existsByApplicationIdAndTargetEnvironmentAndStatusIn(String applicationId,
                                                                  Environment targetEnvironment,
                                                                  Collection<PromotionStatus> statuses);

    /**
     * Checks whether a Promotion exists for the given application, version, and target
     * environment with the given status.
     */
    boolean existsByApplicationIdAndVersionAndTargetEnvironmentAndStatus(String applicationId,
                                                                          String version,
                                                                          Environment targetEnvironment,
                                                                          PromotionStatus status);
}
