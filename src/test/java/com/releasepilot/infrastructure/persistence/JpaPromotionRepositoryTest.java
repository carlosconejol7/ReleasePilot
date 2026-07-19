package com.releasepilot.infrastructure.persistence;

import com.releasepilot.domain.model.ApplicationId;
import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.Promotion;
import com.releasepilot.domain.model.PromotionId;
import com.releasepilot.domain.model.PromotionStatus;
import com.releasepilot.domain.model.User;
import com.releasepilot.domain.model.Version;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JpaPromotionRepository}, verifying that it correctly delegates to
 * {@link PromotionJpaRepository} and maps between domain and persistence representations,
 * without requiring a real database.
 */
@ExtendWith(MockitoExtension.class)
class JpaPromotionRepositoryTest {

    @Mock
    private PromotionJpaRepository jpaRepository;

    private static final User REQUESTER = new User("user-1", false);

    @Test
    void should_SaveMappedEntity_When_SavingPromotion() {
        // Given
        JpaPromotionRepository repository = new JpaPromotionRepository(jpaRepository);
        Promotion promotion = Promotion.request(
                new ApplicationId("app-1"), new Version("1.0.0"), Environment.DEV, Environment.STAGING, REQUESTER, true, false);

        // When
        repository.save(promotion);

        // Then
        ArgumentCaptor<PromotionJpaEntity> captor = ArgumentCaptor.forClass(PromotionJpaEntity.class);
        verify(jpaRepository, times(1)).save(captor.capture());
        assertEquals(promotion.getId().value(), captor.getValue().getId());
    }

    @Test
    void should_ReturnMappedPromotion_When_FoundById() {
        // Given
        JpaPromotionRepository repository = new JpaPromotionRepository(jpaRepository);
        PromotionJpaEntity entity = new PromotionJpaEntity(
                "promotion-1", "app-1", "1.0.0", Environment.DEV, Environment.STAGING,
                PromotionStatus.REQUESTED, "user-1", false);
        when(jpaRepository.findById("promotion-1")).thenReturn(Optional.of(entity));

        // When
        Optional<Promotion> result = repository.findById(new PromotionId("promotion-1"));

        // Then
        assertTrue(result.isPresent());
        assertEquals("promotion-1", result.get().getId().value());
        assertEquals(PromotionStatus.REQUESTED, result.get().getStatus());
    }

    @Test
    void should_ReturnEmpty_When_NotFoundById() {
        // Given
        JpaPromotionRepository repository = new JpaPromotionRepository(jpaRepository);
        when(jpaRepository.findById("unknown")).thenReturn(Optional.empty());

        // When
        Optional<Promotion> result = repository.findById(new PromotionId("unknown"));

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void should_DelegateToExistsQuery_When_CheckingActivePromotion() {
        // Given
        JpaPromotionRepository repository = new JpaPromotionRepository(jpaRepository);
        when(jpaRepository.existsByApplicationIdAndTargetEnvironmentAndStatusIn(
                eq("app-1"), eq(Environment.STAGING), any(List.class))).thenReturn(true);

        // When
        boolean result = repository.hasActivePromotion(new ApplicationId("app-1"), Environment.STAGING);

        // Then
        assertTrue(result);
        verify(jpaRepository, times(1))
                .existsByApplicationIdAndTargetEnvironmentAndStatusIn(eq("app-1"), eq(Environment.STAGING), any(List.class));
    }

    @Test
    void should_DelegateToExistsQuery_When_CheckingVersionCompletedEnvironment() {
        // Given
        JpaPromotionRepository repository = new JpaPromotionRepository(jpaRepository);
        when(jpaRepository.existsByApplicationIdAndVersionAndTargetEnvironmentAndStatus(
                "app-1", "1.0.0", Environment.DEV, PromotionStatus.COMPLETED)).thenReturn(true);

        // When
        boolean result = repository.hasVersionCompletedEnvironment(
                new ApplicationId("app-1"), new Version("1.0.0"), Environment.DEV);

        // Then
        assertTrue(result);
    }
}
