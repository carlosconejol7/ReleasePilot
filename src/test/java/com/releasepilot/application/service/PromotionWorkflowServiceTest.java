package com.releasepilot.application.service;

import com.releasepilot.application.model.WorkItem;
import com.releasepilot.application.port.DeploymentPort;
import com.releasepilot.application.port.IssueTrackerPort;
import com.releasepilot.application.port.NotificationPort;
import com.releasepilot.domain.event.PromotionApproved;
import com.releasepilot.domain.event.PromotionCancelled;
import com.releasepilot.domain.event.PromotionCompleted;
import com.releasepilot.domain.event.PromotionRequested;
import com.releasepilot.domain.event.PromotionRolledBack;
import com.releasepilot.domain.event.PromotionStarted;
import com.releasepilot.domain.model.ApplicationId;
import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.Promotion;
import com.releasepilot.domain.model.PromotionId;
import com.releasepilot.domain.model.User;
import com.releasepilot.domain.model.Version;
import com.releasepilot.domain.repository.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PromotionWorkflowService}, verifying that the correct application
 * ports are invoked in reaction to each {@link com.releasepilot.domain.event.PromotionEvent} type.
 */
@ExtendWith(MockitoExtension.class)
class PromotionWorkflowServiceTest {

    @Mock
    private DeploymentPort deploymentPort;

    @Mock
    private IssueTrackerPort issueTrackerPort;

    @Mock
    private NotificationPort notificationPort;

    @Mock
    private PromotionRepository promotionRepository;

    private static final User REQUESTER = new User("user-1", false);
    private static final User APPROVER = new User("user-2", true);
    private static final User OPERATOR = new User("system-operator", true);

    @Test
    void should_FetchWorkItemsAndNotify_When_PromotionRequested() {
        // Given
        PromotionWorkflowService service = new PromotionWorkflowService(deploymentPort, issueTrackerPort, notificationPort, promotionRepository);
        when(issueTrackerPort.getLinkedWorkItems("promotion-1")).thenReturn(List.of(new WorkItem("ISSUE-1", "Fix bug", "http://tracker/ISSUE-1")));
        PromotionRequested event = new PromotionRequested("promotion-1", "app-1", "1.0.0", Environment.DEV, Environment.STAGING, REQUESTER, Instant.now());

        // When
        service.on(event);

        // Then
        verify(issueTrackerPort, times(1)).getLinkedWorkItems("promotion-1");
        verify(notificationPort, times(1)).notify(eq("promotion-1"), anyString());
        verify(deploymentPort, never()).triggerDeployment(anyString(), any());
    }

    @Test
    void should_Notify_When_PromotionApproved() {
        // Given
        PromotionWorkflowService service = new PromotionWorkflowService(deploymentPort, issueTrackerPort, notificationPort, promotionRepository);
        PromotionApproved event = new PromotionApproved("promotion-1", APPROVER, Instant.now());

        // When
        service.on(event);

        // Then
        verify(notificationPort, times(1)).notify(eq("promotion-1"), anyString());
        verify(deploymentPort, never()).triggerDeployment(anyString(), any());
        verify(issueTrackerPort, never()).getLinkedWorkItems(anyString());
    }

    @Test
    void should_TriggerDeploymentAndNotify_When_PromotionStarted() {
        // Given
        PromotionWorkflowService service = new PromotionWorkflowService(deploymentPort, issueTrackerPort, notificationPort, promotionRepository);
        Promotion promotion = Promotion.request(new ApplicationId("app-1"), new Version("1.0.0"), Environment.DEV, Environment.STAGING, REQUESTER, true, false);
        when(promotionRepository.findById(new PromotionId(promotion.getId().value()))).thenReturn(Optional.of(promotion));
        PromotionStarted event = new PromotionStarted(promotion.getId().value(), OPERATOR, Instant.now());

        // When
        service.on(event);

        // Then
        verify(deploymentPort, times(1)).triggerDeployment(promotion.getId().value(), Environment.STAGING);
        verify(notificationPort, times(1)).notify(eq(promotion.getId().value()), anyString());
    }

    @Test
    void should_NotTriggerDeployment_When_PromotionStartedButPromotionNotFound() {
        // Given
        PromotionWorkflowService service = new PromotionWorkflowService(deploymentPort, issueTrackerPort, notificationPort, promotionRepository);
        when(promotionRepository.findById(any(PromotionId.class))).thenReturn(Optional.empty());
        PromotionStarted event = new PromotionStarted("unknown-promotion", OPERATOR, Instant.now());

        // When
        service.on(event);

        // Then
        verify(deploymentPort, never()).triggerDeployment(anyString(), any());
        verify(notificationPort, times(1)).notify(eq("unknown-promotion"), anyString());
    }

    @Test
    void should_Notify_When_PromotionCompleted() {
        // Given
        PromotionWorkflowService service = new PromotionWorkflowService(deploymentPort, issueTrackerPort, notificationPort, promotionRepository);
        PromotionCompleted event = new PromotionCompleted("promotion-1", OPERATOR, Instant.now());

        // When
        service.on(event);

        // Then
        verify(notificationPort, times(1)).notify(eq("promotion-1"), anyString());
    }

    @Test
    void should_Notify_When_PromotionCancelled() {
        // Given
        PromotionWorkflowService service = new PromotionWorkflowService(deploymentPort, issueTrackerPort, notificationPort, promotionRepository);
        PromotionCancelled event = new PromotionCancelled("promotion-1", REQUESTER, "no longer needed", Instant.now());

        // When
        service.on(event);

        // Then
        verify(notificationPort, times(1)).notify(eq("promotion-1"), anyString());
    }

    @Test
    void should_Notify_When_PromotionRolledBack() {
        // Given
        PromotionWorkflowService service = new PromotionWorkflowService(deploymentPort, issueTrackerPort, notificationPort, promotionRepository);
        PromotionRolledBack event = new PromotionRolledBack("promotion-1", OPERATOR, "deployment failed", Instant.now());

        // When
        service.on(event);

        // Then
        verify(notificationPort, times(1)).notify(eq("promotion-1"), anyString());
    }
}
