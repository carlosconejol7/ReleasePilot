package com.releasepilot.application.service;

import com.releasepilot.application.model.WorkItem;
import com.releasepilot.application.port.DeploymentPort;
import com.releasepilot.application.port.IssueTrackerPort;
import com.releasepilot.application.port.NotificationPort;
import com.releasepilot.domain.event.PromotionApproved;
import com.releasepilot.domain.event.PromotionCancelled;
import com.releasepilot.domain.event.PromotionCompleted;
import com.releasepilot.domain.event.PromotionEvent;
import com.releasepilot.domain.event.PromotionRequested;
import com.releasepilot.domain.event.PromotionRolledBack;
import com.releasepilot.domain.event.PromotionStarted;
import com.releasepilot.domain.model.Promotion;
import com.releasepilot.domain.model.PromotionId;
import com.releasepilot.domain.repository.PromotionRepository;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Application service that orchestrates side effects (deployment triggering, issue tracker
 * lookups, notifications) in reaction to {@link PromotionEvent}s.
 *
 * <p>This service exists specifically so that the {@link Promotion} aggregate can remain a pure
 * state machine, free of any dependency on external systems. All cross-cutting orchestration of
 * {@link DeploymentPort}, {@link IssueTrackerPort}, and {@link NotificationPort} happens here,
 * driven exclusively by already-published domain events.</p>
 *
 * <p><b>Runs asynchronously:</b> event handling in this class is {@code @Async}, meaning it
 * executes on a separate thread from the one that published the event. This ensures that the
 * API request which triggered a Promotion state transition is never blocked waiting on
 * potentially slow external integrations (deployment systems, issue trackers, notification
 * channels); the HTTP response returns as soon as the aggregate has been persisted and
 * projected, while these side effects complete independently in the background. Requires
 * {@code @EnableAsync} to be active in the application context (see
 * {@code com.releasepilot.infrastructure.config.AsyncConfig}) for the {@link Async} annotation
 * to actually take effect.</p>
 */
@Component
public class PromotionProcessManager {

    private final DeploymentPort deploymentPort;
    private final IssueTrackerPort issueTrackerPort;
    private final NotificationPort notificationPort;
    private final PromotionRepository promotionRepository;

    public PromotionProcessManager(DeploymentPort deploymentPort,
                                   IssueTrackerPort issueTrackerPort,
                                   NotificationPort notificationPort,
                                   PromotionRepository promotionRepository) {
        this.deploymentPort = deploymentPort;
        this.issueTrackerPort = issueTrackerPort;
        this.notificationPort = notificationPort;
        this.promotionRepository = promotionRepository;
    }

    /**
     * Handles an incoming {@link PromotionEvent}, exhaustively dispatching on its concrete type
     * via pattern matching over the sealed hierarchy, and triggering the relevant ports.
     *
     * <p>Runs asynchronously on a separate thread from the publishing thread (see class
     * Javadoc for rationale).</p>
     *
     * @param event the domain event to react to
     */
    @Async
    @EventListener
    public void on(PromotionEvent event) {
        switch (event) {
            case PromotionRequested e -> handleRequested(e);
            case PromotionApproved e -> handleApproved(e);
            case PromotionStarted e -> handleStarted(e);
            case PromotionCompleted e -> handleCompleted(e);
            case PromotionCancelled e -> handleCancelled(e);
            case PromotionRolledBack e -> handleRolledBack(e);
        }
    }

    private void handleRequested(PromotionRequested event) {
        List<WorkItem> workItems = issueTrackerPort.getLinkedWorkItems(event.promotionId());
        notificationPort.notify(event.promotionId(), "Promotion requested for " + event.applicationId()
                + " v" + event.version() + " (" + event.sourceEnvironment() + " -> " + event.targetEnvironment()
                + "). Linked work items: " + workItems.size());
    }

    private void handleApproved(PromotionApproved event) {
        notificationPort.notify(event.promotionId(), "Promotion approved by " + event.actor().id());
    }

    private void handleStarted(PromotionStarted event) {
        promotionRepository.findById(new PromotionId(event.promotionId()))
                .ifPresent(promotion -> deploymentPort.triggerDeployment(event.promotionId(), promotion.getTargetEnvironment()));
        notificationPort.notify(event.promotionId(), "Deployment started by " + event.actor().id());
    }

    private void handleCompleted(PromotionCompleted event) {
        notificationPort.notify(event.promotionId(), "Deployment completed by " + event.actor().id());
    }

    private void handleCancelled(PromotionCancelled event) {
        notificationPort.notify(event.promotionId(), "Promotion cancelled by " + event.actor().id() + ": " + event.reason());
    }

    private void handleRolledBack(PromotionRolledBack event) {
        notificationPort.notify(event.promotionId(), "Promotion rolled back by " + event.actor().id() + ": " + event.reason());
    }
}
