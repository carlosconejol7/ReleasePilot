package com.releasepilot.infrastructure.persistence;

import com.releasepilot.domain.model.ApplicationId;
import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.Promotion;
import com.releasepilot.domain.model.PromotionStatus;
import com.releasepilot.domain.model.User;
import com.releasepilot.domain.model.Version;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link PromotionEntityMapper}, verifying lossless round-tripping between
 * the {@link Promotion} domain aggregate and its {@link PromotionJpaEntity} representation.
 */
class PromotionEntityMapperTest {

    private static final User REQUESTER = new User("user-1", false);
    private static final User APPROVER = new User("user-2", true);

    @Test
    void should_MapAllFields_When_ConvertingRequestedPromotionToEntity() {
        // Given
        Promotion promotion = Promotion.request(
                new ApplicationId("app-1"), new Version("1.0.0"), Environment.DEV, Environment.STAGING, REQUESTER, true, false);

        // When
        PromotionJpaEntity entity = PromotionEntityMapper.toEntity(promotion);

        // Then
        assertEquals(promotion.getId().value(), entity.getId());
        assertEquals("app-1", entity.getApplicationId());
        assertEquals("1.0.0", entity.getVersion());
        assertEquals(Environment.DEV, entity.getSourceEnvironment());
        assertEquals(Environment.STAGING, entity.getTargetEnvironment());
        assertEquals(PromotionStatus.REQUESTED, entity.getStatus());
        assertEquals("user-1", entity.getRequestedById());
        assertEquals(false, entity.isRequestedByIsApprover());
    }

    @Test
    void should_ReconstituteEquivalentAggregate_When_ConvertingEntityBackToDomain() {
        // Given
        Promotion original = Promotion.request(
                new ApplicationId("app-1"), new Version("1.0.0"), Environment.DEV, Environment.STAGING, REQUESTER, true, false);
        original.approve(APPROVER);
        PromotionJpaEntity entity = PromotionEntityMapper.toEntity(original);

        // When
        Promotion rehydrated = PromotionEntityMapper.toDomain(entity);

        // Then
        assertEquals(original.getId(), rehydrated.getId());
        assertEquals(original.getApplicationId(), rehydrated.getApplicationId());
        assertEquals(original.getVersion(), rehydrated.getVersion());
        assertEquals(original.getSourceEnvironment(), rehydrated.getSourceEnvironment());
        assertEquals(original.getTargetEnvironment(), rehydrated.getTargetEnvironment());
        assertEquals(original.getStatus(), rehydrated.getStatus());
        assertEquals(original.getRequestedBy(), rehydrated.getRequestedBy());
    }

    @Test
    void should_ProduceNoDomainEvents_When_Reconstituting() {
        // Given
        Promotion original = Promotion.request(
                new ApplicationId("app-1"), new Version("1.0.0"), Environment.DEV, Environment.STAGING, REQUESTER, true, false);
        PromotionJpaEntity entity = PromotionEntityMapper.toEntity(original);

        // When
        Promotion rehydrated = PromotionEntityMapper.toDomain(entity);

        // Then
        assertEquals(0, rehydrated.pullDomainEvents().size());
    }
}
