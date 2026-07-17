package com.releasepilot.application.command;

import com.releasepilot.domain.exception.DomainException;
import com.releasepilot.domain.model.ApplicationId;
import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.Promotion;
import com.releasepilot.domain.model.PromotionId;
import com.releasepilot.domain.model.PromotionStatus;
import com.releasepilot.domain.model.Version;
import com.releasepilot.domain.repository.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
 * <p>Each handler is verified using a real {@link Promotion} aggregate instance
 * (built via {@link Promotion#request}), so that the actual domain state machine
 * is exercised rather than a mocked stand-in. This ensures that any bugs in the
 * aggregate's transition logic are caught by these application-layer tests.</p>
 *
 * <p>Each handler is verified for both the happy path (state actually mutated and
 * persisted) and the not-found path (a {@link DomainException} is thrown and
 * nothing is persisted).</p>
 */
@ExtendWith(MockitoExtension.class)
class PromotionLifecycleCommandHandlersTest {

    @Mock
    private PromotionRepository repository;

    private static final String PROMOTION_ID_VALUE = "promotion-1";

    private Promotion newRequestedPromotion() {
        ApplicationId applicationId = new ApplicationId("app-1");
        Version version = new Version("1.0.0");
        return Promotion.request(applicationId, version, Environment.DEV, Environment.STAGING, "user-1", true, false);
    }

    private Promotion newApprovedPromotion() {
        Promotion promotion = newRequestedPromotion();
        promotion.approve("user-2");
        return promotion;
    }

    private Promotion newDeploymentStartedPromotion() {
        Promotion promotion = newApprovedPromotion();
        promotion.startDeployment("system-operator");
        return promotion;
    }

    // --- ApprovePromotionCommandHandler ---

    @Test
    void should_ApproveAndSavePromotion_When_PromotionExists() {
        // Given
        Promotion promotion = newRequestedPromotion();
        when(repository.findById(eq(promotion.getId()))).thenReturn(Optional.of(promotion));
        ApprovePromotionCommandHandler handler = new ApprovePromotionCommandHandler(repository);
        ApprovePromotionCommand command = new ApprovePromotionCommand(promotion.getId().value(), "user-2");

        // When
        handler.handle(command);

        // Then
        assertEquals(PromotionStatus.APPROVED, promotion.getStatus());
        verify(repository, times(1)).save(promotion);
    }

    @Test
    void should_ThrowDomainException_When_ApprovingNonExistentPromotion() {
        // Given
        PromotionId promotionId = new PromotionId(PROMOTION_ID_VALUE);
        when(repository.findById(eq(promotionId))).thenReturn(Optional.empty());
        ApprovePromotionCommandHandler handler = new ApprovePromotionCommandHandler(repository);
        ApprovePromotionCommand command = new ApprovePromotionCommand(PROMOTION_ID_VALUE, "user-2");

        // When / Then
        assertThrows(DomainException.class, () -> handler.handle(command));
        verify(repository, never()).save(any());
    }

    // --- StartDeploymentCommandHandler ---

    @Test
    void should_StartDeploymentAndSavePromotion_When_PromotionExists() {
        // Given
        Promotion promotion = newApprovedPromotion();
        when(repository.findById(eq(promotion.getId()))).thenReturn(Optional.of(promotion));
        StartDeploymentCommandHandler handler = new StartDeploymentCommandHandler(repository);
        StartDeploymentCommand command = new StartDeploymentCommand(promotion.getId().value(), "system-operator");

        // When
        handler.handle(command);

        // Then
        assertEquals(PromotionStatus.DEPLOYMENT_STARTED, promotion.getStatus());
        verify(repository, times(1)).save(promotion);
    }

    @Test
    void should_ThrowDomainException_When_StartingDeploymentForNonExistentPromotion() {
        // Given
        PromotionId promotionId = new PromotionId(PROMOTION_ID_VALUE);
        when(repository.findById(eq(promotionId))).thenReturn(Optional.empty());
        StartDeploymentCommandHandler handler = new StartDeploymentCommandHandler(repository);
        StartDeploymentCommand command = new StartDeploymentCommand(PROMOTION_ID_VALUE, "system-operator");

        // When / Then
        assertThrows(DomainException.class, () -> handler.handle(command));
        verify(repository, never()).save(any());
    }

    // --- CompletePromotionCommandHandler ---

    @Test
    void should_CompleteDeploymentAndSavePromotion_When_PromotionExists() {
        // Given
        Promotion promotion = newDeploymentStartedPromotion();
        when(repository.findById(eq(promotion.getId()))).thenReturn(Optional.of(promotion));
        CompletePromotionCommandHandler handler = new CompletePromotionCommandHandler(repository);
        CompletePromotionCommand command = new CompletePromotionCommand(promotion.getId().value(), "system-operator");

        // When
        handler.handle(command);

        // Then
        assertEquals(PromotionStatus.COMPLETED, promotion.getStatus());
        verify(repository, times(1)).save(promotion);
    }

    @Test
    void should_ThrowDomainException_When_CompletingDeploymentForNonExistentPromotion() {
        // Given
        PromotionId promotionId = new PromotionId(PROMOTION_ID_VALUE);
        when(repository.findById(eq(promotionId))).thenReturn(Optional.empty());
        CompletePromotionCommandHandler handler = new CompletePromotionCommandHandler(repository);
        CompletePromotionCommand command = new CompletePromotionCommand(PROMOTION_ID_VALUE, "system-operator");

        // When / Then
        assertThrows(DomainException.class, () -> handler.handle(command));
        verify(repository, never()).save(any());
    }

    // --- CancelPromotionCommandHandler ---

    @Test
    void should_CancelAndSavePromotion_When_PromotionExists() {
        // Given
        Promotion promotion = newRequestedPromotion();
        when(repository.findById(eq(promotion.getId()))).thenReturn(Optional.of(promotion));
        CancelPromotionCommandHandler handler = new CancelPromotionCommandHandler(repository);
        CancelPromotionCommand command = new CancelPromotionCommand(promotion.getId().value(), "user-1", "no longer needed");

        // When
        handler.handle(command);

        // Then
        assertEquals(PromotionStatus.CANCELLED, promotion.getStatus());
        verify(repository, times(1)).save(promotion);
    }

    @Test
    void should_ThrowDomainException_When_CancellingNonExistentPromotion() {
        // Given
        PromotionId promotionId = new PromotionId(PROMOTION_ID_VALUE);
        when(repository.findById(eq(promotionId))).thenReturn(Optional.empty());
        CancelPromotionCommandHandler handler = new CancelPromotionCommandHandler(repository);
        CancelPromotionCommand command = new CancelPromotionCommand(PROMOTION_ID_VALUE, "user-1", "no longer needed");

        // When / Then
        assertThrows(DomainException.class, () -> handler.handle(command));
        verify(repository, never()).save(any());
    }

    // --- RollbackPromotionCommandHandler ---

    @Test
    void should_RollbackAndSavePromotion_When_PromotionExists() {
        // Given
        Promotion promotion = newDeploymentStartedPromotion();
        when(repository.findById(eq(promotion.getId()))).thenReturn(Optional.of(promotion));
        RollbackPromotionCommandHandler handler = new RollbackPromotionCommandHandler(repository);
        RollbackPromotionCommand command = new RollbackPromotionCommand(promotion.getId().value(), "system-operator", "deployment failed");

        // When
        handler.handle(command);

        // Then
        assertEquals(PromotionStatus.ROLLED_BACK, promotion.getStatus());
        verify(repository, times(1)).save(promotion);
    }

    @Test
    void should_ThrowDomainException_When_RollingBackNonExistentPromotion() {
        // Given
        PromotionId promotionId = new PromotionId(PROMOTION_ID_VALUE);
        when(repository.findById(eq(promotionId))).thenReturn(Optional.empty());
        RollbackPromotionCommandHandler handler = new RollbackPromotionCommandHandler(repository);
        RollbackPromotionCommand command = new RollbackPromotionCommand(PROMOTION_ID_VALUE, "system-operator", "deployment failed");

        // When / Then
        assertThrows(DomainException.class, () -> handler.handle(command));
        verify(repository, never()).save(any());
    }
}
