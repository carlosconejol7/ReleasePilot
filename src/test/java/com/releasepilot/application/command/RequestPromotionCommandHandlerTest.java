package com.releasepilot.application.command;

import com.releasepilot.application.event.DomainEventPublisher;
import com.releasepilot.domain.exception.DomainException;
import com.releasepilot.domain.model.ApplicationId;
import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.PromotionId;
import com.releasepilot.domain.model.Version;
import com.releasepilot.domain.repository.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RequestPromotionCommandHandler}, verifying the orchestration
 * between the {@link PromotionRepository} history checks and the domain invariants
 * enforced by the {@code Promotion} aggregate.
 */
@ExtendWith(MockitoExtension.class)
class RequestPromotionCommandHandlerTest {

    @Mock
    private PromotionRepository repository;

    @Mock
    private DomainEventPublisher publisher;

    @InjectMocks
    private RequestPromotionCommandHandler handler;

    @Test
    void should_CreateAndSavePromotion_When_CommandIsValid() {
        // Given
        RequestPromotionCommand command = new RequestPromotionCommand("app-1", "1.0.0", Environment.STAGING, Environment.PRODUCTION, "user-1");
        when(repository.hasActivePromotion(any(ApplicationId.class), eq(Environment.PRODUCTION))).thenReturn(false);
        when(repository.hasVersionCompletedEnvironment(any(ApplicationId.class), any(Version.class), eq(Environment.STAGING))).thenReturn(true);

        // When
        PromotionId promotionId = handler.handle(command);

        // Then
        assertNotNull(promotionId);
        verify(repository, times(1)).save(any());
        verify(publisher, times(1)).publish(any());
    }

    @Test
    void should_PropagateException_When_TargetEnvironmentAlreadyHasActivePromotion() {
        // Given
        RequestPromotionCommand command = new RequestPromotionCommand("app-1", "1.0.0", Environment.DEV, Environment.STAGING, "user-1");
        when(repository.hasActivePromotion(any(ApplicationId.class), eq(Environment.STAGING))).thenReturn(true);

        // When / Then
        assertThrows(DomainException.class, () -> handler.handle(command));
        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void should_PropagateException_When_VersionHasNotCompletedSourceEnvironment() {
        // Given
        RequestPromotionCommand command = new RequestPromotionCommand("app-1", "1.0.0", Environment.STAGING, Environment.PRODUCTION, "user-1");
        when(repository.hasActivePromotion(any(ApplicationId.class), eq(Environment.PRODUCTION))).thenReturn(false);
        when(repository.hasVersionCompletedEnvironment(any(ApplicationId.class), any(Version.class), eq(Environment.STAGING))).thenReturn(false);

        // When / Then
        assertThrows(DomainException.class, () -> handler.handle(command));
        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void should_AllowPromotionFromDev_WithoutCheckingHistory() {
        // Given
        RequestPromotionCommand command = new RequestPromotionCommand("app-1", "1.0.0", Environment.DEV, Environment.STAGING, "user-1");
        when(repository.hasActivePromotion(any(ApplicationId.class), eq(Environment.STAGING))).thenReturn(false);

        // When
        PromotionId promotionId = handler.handle(command);

        // Then
        assertNotNull(promotionId);
        verify(repository, times(1)).save(any());
        verify(repository, never()).hasVersionCompletedEnvironment(any(), any(), any());
        verify(publisher, times(1)).publish(any(com.releasepilot.domain.event.PromotionRequested.class));
    }
}
