package com.releasepilot.infrastructure.persistence;

import com.releasepilot.domain.model.ApplicationId;
import com.releasepilot.domain.model.Promotion;
import com.releasepilot.domain.model.PromotionId;
import com.releasepilot.domain.model.User;
import com.releasepilot.domain.model.Version;

/**
 * Stateless mapper translating between the {@link Promotion} domain aggregate and its
 * {@link PromotionJpaEntity} persistence representation.
 *
 * <p>Kept entirely separate from both the aggregate and the entity, so that neither needs
 * to be aware of the other's shape. Package-private: only {@link JpaPromotionRepository}
 * should use it.</p>
 */
final class PromotionEntityMapper {

    private PromotionEntityMapper() {
    }

    /**
     * Maps a {@link Promotion} aggregate to its {@link PromotionJpaEntity} representation.
     *
     * @param promotion the aggregate to map
     * @return the corresponding JPA entity
     */
    static PromotionJpaEntity toEntity(Promotion promotion) {
        return new PromotionJpaEntity(
                promotion.getId().value(),
                promotion.getApplicationId().value(),
                promotion.getVersion().value(),
                promotion.getSourceEnvironment(),
                promotion.getTargetEnvironment(),
                promotion.getStatus(),
                promotion.getRequestedBy().id(),
                promotion.getRequestedBy().isApprover()
        );
    }

    /**
     * Maps a {@link PromotionJpaEntity} back to a {@link Promotion} aggregate, via
     * {@link Promotion#reconstitute}.
     *
     * @param entity the JPA entity to map
     * @return the corresponding rehydrated aggregate
     */
    static Promotion toDomain(PromotionJpaEntity entity) {
        return Promotion.reconstitute(
                new PromotionId(entity.getId()),
                new ApplicationId(entity.getApplicationId()),
                new Version(entity.getVersion()),
                entity.getSourceEnvironment(),
                entity.getTargetEnvironment(),
                new User(entity.getRequestedById(), entity.isRequestedByIsApprover()),
                entity.getStatus()
        );
    }
}
