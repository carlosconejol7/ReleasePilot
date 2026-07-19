package com.releasepilot.infrastructure.persistence;

import com.releasepilot.domain.model.ApplicationId;
import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.Promotion;
import com.releasepilot.domain.model.PromotionId;
import com.releasepilot.domain.model.PromotionStatus;
import com.releasepilot.domain.model.Version;
import com.releasepilot.domain.repository.PromotionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Concrete implementation of the domain's {@link PromotionRepository} port, backed by
 * PostgreSQL via {@link PromotionJpaRepository}.
 *
 * <p>Bridges the {@link Promotion} aggregate and its {@link PromotionJpaEntity} persistence
 * representation through {@link PromotionEntityMapper}, keeping the aggregate itself free of
 * any persistence concerns.</p>
 */
@Repository
public class JpaPromotionRepository implements PromotionRepository {

    private static final List<PromotionStatus> ACTIVE_STATUSES = List.of(
            PromotionStatus.REQUESTED,
            PromotionStatus.APPROVED,
            PromotionStatus.DEPLOYMENT_STARTED
    );

    private final PromotionJpaRepository jpaRepository;

    public JpaPromotionRepository(PromotionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Promotion promotion) {
        jpaRepository.save(PromotionEntityMapper.toEntity(promotion));
    }

    @Override
    public Optional<Promotion> findById(PromotionId id) {
        return jpaRepository.findById(id.value()).map(PromotionEntityMapper::toDomain);
    }

    @Override
    public boolean hasActivePromotion(ApplicationId applicationId, Environment target) {
        return jpaRepository.existsByApplicationIdAndTargetEnvironmentAndStatusIn(
                applicationId.value(), target, ACTIVE_STATUSES);
    }

    @Override
    public boolean hasVersionCompletedEnvironment(ApplicationId applicationId, Version version, Environment environment) {
        return jpaRepository.existsByApplicationIdAndVersionAndTargetEnvironmentAndStatus(
                applicationId.value(), version.value(), environment, PromotionStatus.COMPLETED);
    }
}
