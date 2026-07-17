package com.releasepilot.application.command;

import com.releasepilot.domain.exception.DomainException;
import com.releasepilot.domain.model.Promotion;
import com.releasepilot.domain.model.PromotionId;
import com.releasepilot.domain.repository.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Promotion lifecycle command handlers
 * ({@link ApprovePromotionCommandHandler}, {@link StartDeploymentCommandHandler},
 * {@link CompletePromotionCommandHandler}, {@link CancelPromotionCommandHandler},
 * and {@link RollbackPromotionCommandHandler}).
 *
 * <p>Each handler is verified for both the happy path (mutation invoked and persisted)
 * and the not-found path (a {@link DomainException} is thrown and nothing is persisted).</p>
 */
@ExtendWith(MockitoExtension.class)
class PromotionLifecycleCommandHandlersTest {

    @Mock
    private PromotionRepository repository;

    private static final String PROMOTION_ID = "promotion-1";

    // --- ApprovePromotionCommandHandler ---

    @Test
    void should_ApproveAndSavePromotion_When_PromotionExists() {
        // Given
        Promotion promotion = mock(Promotion.class);
        when(repository.findById(eq(new PromotionId(PROMOTION_ID)))).thenReturn(Optional.of(promotion));
        ApprovePromotionCommandHandler handler = new ApprovePromotionCommandHandler(repository);
        ApprovePromotionCommand command = new ApprovePromotionCommand(PROMOTION_ID, "user-2");

        // When
        handler.handle(command);

        // Then
        verify(promotion, times(1)).approve("user-2");
        verify(repository, times(1)).save(promotion);
    }

    @Test
    void should_ThrowDomainException_When_ApprovingNonExistentPromotion() {
        // Given
        when(repository.findById(eq(new PromotionId(PROMOTION_ID)))).thenReturn(Optional.empty());
        ApprovePromotionCommandHandler handler = new ApprovePromotionCommandHandler(repository);
        ApprovePromotionCommand command = new ApprovePromotionCommand(PROMOTION_ID, "user-2");

        // When / Then
        assertThrows(DomainException.class, () -> handler.handle(command));
        verify(repository, never()).save(any());
    }

    // --- StartDeploymentCommandHandler ---

    @Test
    void should_StartDeploymentAndSavePromotion_When_PromotionExists() {
        // Given
        Promotion promotion = mock(Promotion.class);
        when(repository.findById(eq(new PromotionId(PROMOTION_ID)))).thenReturn(Optional.of(promotion));
        StartDeploymentCommandHandler handler = new StartDeploymentCommandHandler(repository);
        StartDeploymentCommand command = new StartDeploymentCommand(PROMOTION_ID, "system-operator");

        // When
        handler.handle(command);

        // Then
        verify(promotion, times(1)).startDeployment("system-operator");
        verify(repository, times(1)).save(promotion);
    }

    @Test
    void should_ThrowDomainException_When_StartingDeploymentForNonExistentPromotion() {
        // Given
        when(repository.findById(eq(new PromotionId(PROMOTION_ID)))).thenReturn(Optional.empty());
        StartDeploymentCommandHandler handler = new StartDeploymentCommandHandler(repository);
        StartDeploymentCommand command = new StartDeploymentCommand(PROMOTION_ID, "system-operator");

        // When / Then
        assertThrows(DomainException.class, () -> handler.handle(command));
        verify(repository, never()).save(any());
    }

    // --- CompletePromotionCommandHandler ---

    @Test
    void should_CompleteDeploymentAndSavePromotion_When_PromotionExists() {
        // Given
        Promotion promotion = mock(Promotion.class);
        when(repository.findById(eq(new PromotionId(PROMOTION_ID)))).thenReturn(Optional.of(promotion));
        CompletePromotionCommandHandler handler = new CompletePromotionCommandHandler(repository);
        CompletePromotionCommand command = new CompletePromotionCommand(PROMOTION_ID, "system-operator");

        // When
        handler.handle(command);

        // Then
        verify(promotion, times(1)).completeDeployment("system-operator");
        verify(repository, times(1)).save(promotion);
    }

    @Test
    void should_ThrowDomainException_When_CompletingDeploymentForNonExistentPromotion() {
        // Given
        when(repository.findById(eq(new PromotionId(PROMOTION_ID)))).thenReturn(Optional.empty());
        CompletePromotionCommandHandler handler = new CompletePromotionCommandHandler(repository);
        CompletePromotionCommand command = new CompletePromotionCommand(PROMOTION_ID, "system-operator");

        // When / Then
        assertThrows(DomainException.class, () -> handler.handle(command));
        verify(repository, never()).save(any());
    }

    // --- CancelPromotionCommandHandler ---

    @Test
    void should_CancelAndSavePromotion_When_PromotionExists() {
        // Given
        Promotion promotion = mock(Promotion.class);
        when(repository.findById(eq(new PromotionId(PROMOTION_ID)))).thenReturn(Optional.of(promotion));
        CancelPromotionCommandHandler handler = new CancelPromotionCommandHandler(repository);
        CancelPromotionCommand command = new CancelPromotionCommand(PROMOTION_ID, "user-1", "no longer needed");

        // When
        handler.handle(command);

        // Then
        verify(promotion, times(1)).cancel("user-1", "no longer needed");
        verify(repository, times(1)).save(promotion);
    }

    @Test
    void should_ThrowDomainException_When_CancellingNonExistentPromotion() {
        // Given
        when(repository.findById(eq(new PromotionId(PROMOTION_ID)))).thenReturn(Optional.empty());
        CancelPromotionCommandHandler handler = new CancelPromotionCommandHandler(repository);
        CancelPromotionCommand command = new CancelPromotionCommand(PROMOTION_ID, "user-1", "no longer needed");

        // When / Then
        assertThrows(DomainException.class, () -> handler.handle(command));
        verify(repository, never()).save(any());
    }

    // --- RollbackPromotionCommandHandler ---

    @Test
    void should_RollbackAndSavePromotion_When_PromotionExists() {
        // Given
        Promotion promotion = mock(Promotion.class);
        when(repository.findById(eq(new PromotionId(PROMOTION_ID)))).thenReturn(Optional.of(promotion));
        RollbackPromotionCommandHandler handler = new RollbackPromotionCommandHandler(repository);
        RollbackPromotionCommand command = new RollbackPromotionCommand(PROMOTION_ID, "system-operator", "deployment failed");

        // When
        handler.handle(command);

        // Then
        verify(promotion, times(1)).rollback("system-operator", "deployment failed");
        verify(repository, times(1)).save(promotion);
    }

    @Test
    void should_ThrowDomainException_When_RollingBackNonExistentPromotion() {
        // Given
        when(repository.findById(eq(new PromotionId(PROMOTION_ID)))).thenReturn(Optional.empty());
        RollbackPromotionCommandHandler handler = new RollbackPromotionCommandHandler(repository);
        RollbackPromotionCommand command = new RollbackPromotionCommand(PROMOTION_ID, "system-operator", "deployment failed");

        // When / Then
        assertThrows(DomainException.class, () -> handler.handle(command));
        verify(repository, never()).save(any());
    }
}
