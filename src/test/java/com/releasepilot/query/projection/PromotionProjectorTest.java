package com.releasepilot.query.projection;

import com.releasepilot.domain.event.PromotionApproved;
import com.releasepilot.domain.event.PromotionCancelled;
import com.releasepilot.domain.event.PromotionCompleted;
import com.releasepilot.domain.event.PromotionRequested;
import com.releasepilot.domain.event.PromotionRolledBack;
import com.releasepilot.domain.event.PromotionStarted;
import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.PromotionStatus;
import com.releasepilot.domain.model.User;
import com.releasepilot.query.model.ApplicationStatusView;
import com.releasepilot.query.model.EnvironmentStatusView;
import com.releasepilot.query.model.PagedResult;
import com.releasepilot.query.model.PromotionSummaryView;
import com.releasepilot.query.model.PromotionView;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PromotionProjector}, verifying that domain events are correctly
 * projected into read models, independently of the write-side aggregate.
 */
class PromotionProjectorTest {

    private static final User REQUESTER = new User("user-1", false);
    private static final User APPROVER = new User("user-2", true);
    private static final User OPERATOR = new User("system-operator", true);

    @Test
    void should_ProjectFullLifecycle_When_PromotionCompletesSuccessfully() {
        // Given
        PromotionProjector projector = new PromotionProjector();
        String promotionId = "promotion-1";
        Instant requestedAt = Instant.parse("2024-01-01T00:00:00Z");

        // When
        projector.on(new PromotionRequested(promotionId, "app-1", "1.0.0", Environment.DEV, Environment.STAGING, REQUESTER, requestedAt));
        projector.on(new PromotionApproved(promotionId, APPROVER, requestedAt.plusSeconds(60)));
        projector.on(new PromotionStarted(promotionId, OPERATOR, requestedAt.plusSeconds(120)));
        projector.on(new PromotionCompleted(promotionId, OPERATOR, requestedAt.plusSeconds(180)));

        // Then
        Optional<PromotionView> view = projector.findById(promotionId);
        assertTrue(view.isPresent());
        assertEquals(PromotionStatus.COMPLETED, view.get().status());
        assertEquals(4, view.get().history().size());
        assertEquals(Environment.DEV, view.get().sourceEnvironment());
        assertEquals(Environment.STAGING, view.get().targetEnvironment());

        Optional<ApplicationStatusView> status = projector.findApplicationStatus("app-1");
        assertTrue(status.isPresent());
        assertEquals(1, status.get().environments().size());
        EnvironmentStatusView environmentStatus = status.get().environments().get(0);
        assertEquals(Environment.STAGING, environmentStatus.environment());
        assertEquals(PromotionStatus.COMPLETED, environmentStatus.status());
    }

    @Test
    void should_ProjectCancellationReason_When_PromotionIsCancelled() {
        // Given
        PromotionProjector projector = new PromotionProjector();
        String promotionId = "promotion-2";
        Instant requestedAt = Instant.parse("2024-01-01T00:00:00Z");

        // When
        projector.on(new PromotionRequested(promotionId, "app-1", "1.0.0", Environment.DEV, Environment.STAGING, REQUESTER, requestedAt));
        projector.on(new PromotionCancelled(promotionId, REQUESTER, "no longer needed", requestedAt.plusSeconds(30)));

        // Then
        PromotionView view = projector.findById(promotionId).orElseThrow();
        assertEquals(PromotionStatus.CANCELLED, view.status());
        assertEquals("no longer needed", view.history().get(1).reason());
    }

    @Test
    void should_ProjectRollbackReason_When_PromotionIsRolledBack() {
        // Given
        PromotionProjector projector = new PromotionProjector();
        String promotionId = "promotion-3";
        Instant requestedAt = Instant.parse("2024-01-01T00:00:00Z");

        // When
        projector.on(new PromotionRequested(promotionId, "app-1", "1.0.0", Environment.DEV, Environment.STAGING, REQUESTER, requestedAt));
        projector.on(new PromotionApproved(promotionId, APPROVER, requestedAt.plusSeconds(30)));
        projector.on(new PromotionStarted(promotionId, OPERATOR, requestedAt.plusSeconds(60)));
        projector.on(new PromotionRolledBack(promotionId, OPERATOR, "deployment failed", requestedAt.plusSeconds(90)));

        // Then
        PromotionView view = projector.findById(promotionId).orElseThrow();
        assertEquals(PromotionStatus.ROLLED_BACK, view.status());
        assertEquals("deployment failed", view.history().get(3).reason());
    }

    @Test
    void should_ReturnEmpty_When_PromotionIsUnknown() {
        // Given
        PromotionProjector projector = new PromotionProjector();

        // When / Then
        assertTrue(projector.findById("unknown-promotion").isEmpty());
        assertTrue(projector.findApplicationStatus("unknown-app").isEmpty());
    }

    @Test
    void should_ReturnMostRecentFirst_And_RespectPaging_When_ListingApplicationPromotions() {
        // Given
        PromotionProjector projector = new PromotionProjector();
        Instant base = Instant.parse("2024-01-01T00:00:00Z");

        projector.on(new PromotionRequested("promotion-a", "app-1", "1.0.0", Environment.DEV, Environment.STAGING, REQUESTER, base));
        projector.on(new PromotionRequested("promotion-b", "app-1", "1.1.0", Environment.DEV, Environment.STAGING, REQUESTER, base.plusSeconds(10)));
        projector.on(new PromotionRequested("promotion-c", "app-1", "1.2.0", Environment.DEV, Environment.STAGING, REQUESTER, base.plusSeconds(20)));
        projector.on(new PromotionRequested("promotion-other-app", "app-2", "1.0.0", Environment.DEV, Environment.STAGING, REQUESTER, base.plusSeconds(30)));

        // When
        PagedResult<PromotionSummaryView> firstPage = projector.findPromotionsByApplication("app-1", 0, 2);
        PagedResult<PromotionSummaryView> secondPage = projector.findPromotionsByApplication("app-1", 1, 2);

        // Then
        assertEquals(3, firstPage.totalElements());
        assertEquals(2, firstPage.items().size());
        assertEquals("promotion-c", firstPage.items().get(0).promotionId());
        assertEquals("promotion-b", firstPage.items().get(1).promotionId());

        assertEquals(1, secondPage.items().size());
        assertEquals("promotion-a", secondPage.items().get(0).promotionId());
    }
}
