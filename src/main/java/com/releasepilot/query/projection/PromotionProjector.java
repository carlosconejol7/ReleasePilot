package com.releasepilot.query.projection;

import com.releasepilot.domain.event.PromotionApproved;
import com.releasepilot.domain.event.PromotionCancelled;
import com.releasepilot.domain.event.PromotionCompleted;
import com.releasepilot.domain.event.PromotionEvent;
import com.releasepilot.domain.event.PromotionRequested;
import com.releasepilot.domain.event.PromotionRolledBack;
import com.releasepilot.domain.event.PromotionStarted;
import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.PromotionStatus;
import com.releasepilot.query.model.ApplicationStatusView;
import com.releasepilot.query.model.EnvironmentStatusView;
import com.releasepilot.query.model.PagedResult;
import com.releasepilot.query.model.PromotionHistoryEntry;
import com.releasepilot.query.model.PromotionSummaryView;
import com.releasepilot.query.model.PromotionView;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Read-side projector that subscribes to {@link PromotionEvent}s published on the write side
 * and maintains an in-memory, denormalized representation of Promotion state optimized for
 * querying.
 *
 * <p>This class doubles as the in-memory "read repository": it owns the projected state and
 * exposes query methods directly to the application's query services. It performs no business
 * logic of its own; it only reacts to already-validated domain events and reshapes them into
 * read models.</p>
 *
 * <p><b>Synchronous by design:</b> unlike other listeners of {@link PromotionEvent} (such as
 * {@code PromotionProcessManager} and {@code AuditLogConsumer}, which react asynchronously),
 * this projector deliberately runs <b>synchronously</b>, on the same thread as — and therefore
 * before — the command handler returns its HTTP response. This is a conscious trade-off:
 * it sacrifices a small amount of request latency in exchange for guaranteed read-after-write
 * consistency, so that a client reading a Promotion immediately after writing to it always
 * observes its own write.</p>
 *
 * <p>This trade-off is only viable because the projector currently maintains its read model
 * in local memory. Should this projector be migrated to build a read model in an external
 * store (e.g., Redis or Elasticsearch), synchronous projection would introduce unacceptable
 * latency and I/O coupling into the write path; at that point, this listener should also be
 * made asynchronous, and the read-after-write consistency guarantee explicitly relaxed to
 * eventual consistency (as is already the case for the other, asynchronous listeners).</p>
 */
@Component
public class PromotionProjector {

    private final Map<String, PromotionState> promotionsById = new ConcurrentHashMap<>();
    private final Map<String, Map<Environment, EnvironmentStatusView>> environmentStatusByApplication = new ConcurrentHashMap<>();

    /**
     * Handles an incoming {@link PromotionEvent}, exhaustively dispatching on its concrete type
     * via pattern matching over the sealed hierarchy.
     *
     * <p>Runs synchronously on the publishing thread (see class Javadoc for rationale).</p>
     *
     * @param event the domain event to project
     */
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

    /**
     * Finds the detail view for a single Promotion.
     *
     * @param promotionId the identifier of the promotion
     * @return an {@link Optional} containing the {@link PromotionView} if known, empty otherwise
     */
    public Optional<PromotionView> findById(String promotionId) {
        return Optional.ofNullable(promotionsById.get(promotionId)).map(PromotionState::toView);
    }

    /**
     * Finds the current per-environment status for a given application.
     *
     * @param applicationId the identifier of the application
     * @return an {@link Optional} containing the {@link ApplicationStatusView} if the application
     *         has at least one known promotion, empty otherwise
     */
    public Optional<ApplicationStatusView> findApplicationStatus(String applicationId) {
        Map<Environment, EnvironmentStatusView> environments = environmentStatusByApplication.get(applicationId);
        if (environments == null || environments.isEmpty()) {
            return Optional.empty();
        }
        List<EnvironmentStatusView> sorted = environments.values().stream()
                .sorted(Comparator.comparing(EnvironmentStatusView::environment))
                .toList();
        return Optional.of(new ApplicationStatusView(applicationId, sorted));
    }

    /**
     * Finds a page of the promotion history for a given application, most recently requested first.
     *
     * @param applicationId the identifier of the application
     * @param page          the zero-based page index (negative values are treated as {@code 0})
     * @param size          the maximum number of items per page (values less than {@code 1} are treated as {@code 1})
     * @return a {@link PagedResult} containing the matching {@link PromotionSummaryView} items
     */
    public PagedResult<PromotionSummaryView> findPromotionsByApplication(String applicationId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);

        List<PromotionSummaryView> all = promotionsById.values().stream()
                .filter(state -> state.applicationId.equals(applicationId))
                .sorted(Comparator.comparing((PromotionState state) -> state.requestedAt).reversed())
                .map(PromotionState::toSummary)
                .toList();

        int fromIndex = Math.min(safePage * safeSize, all.size());
        int toIndex = Math.min(fromIndex + safeSize, all.size());

        return new PagedResult<>(all.subList(fromIndex, toIndex), safePage, safeSize, all.size());
    }

    private void handleRequested(PromotionRequested event) {
        PromotionState state = new PromotionState(
                event.promotionId(),
                event.applicationId(),
                event.version(),
                event.sourceEnvironment(),
                event.targetEnvironment(),
                event.actor().id(),
                event.occurredAt()
        );
        state.history.add(new PromotionHistoryEntry(PromotionStatus.REQUESTED, event.actor().id(), event.occurredAt(), null));
        promotionsById.put(state.promotionId, state);
        updateEnvironmentStatus(state);
    }

    private void handleApproved(PromotionApproved event) {
        applyTransition(event.promotionId(), PromotionStatus.APPROVED, event.actor().id(), event.occurredAt(), null);
    }

    private void handleStarted(PromotionStarted event) {
        applyTransition(event.promotionId(), PromotionStatus.DEPLOYMENT_STARTED, event.actor().id(), event.occurredAt(), null);
    }

    private void handleCompleted(PromotionCompleted event) {
        applyTransition(event.promotionId(), PromotionStatus.COMPLETED, event.actor().id(), event.occurredAt(), null);
    }

    private void handleCancelled(PromotionCancelled event) {
        applyTransition(event.promotionId(), PromotionStatus.CANCELLED, event.actor().id(), event.occurredAt(), event.reason());
    }

    private void handleRolledBack(PromotionRolledBack event) {
        applyTransition(event.promotionId(), PromotionStatus.ROLLED_BACK, event.actor().id(), event.occurredAt(), event.reason());
    }

    private void applyTransition(String promotionId, PromotionStatus newStatus, String actor, Instant occurredAt, String reason) {
        PromotionState state = promotionsById.get(promotionId);
        if (state == null) {
            // Defensive: an event referencing an unknown promotion is ignored by the read side,
            // since the write side is the source of truth for validity.
            return;
        }
        state.status = newStatus;
        state.lastUpdatedAt = occurredAt;
        state.history.add(new PromotionHistoryEntry(newStatus, actor, occurredAt, reason));
        updateEnvironmentStatus(state);
    }

    private void updateEnvironmentStatus(PromotionState state) {
        environmentStatusByApplication
                .computeIfAbsent(state.applicationId, key -> new ConcurrentHashMap<>())
                .put(state.targetEnvironment, state.toEnvironmentStatus());
    }

    /**
     * Mutable internal projection state for a single Promotion.
     *
     * <p>Kept private to this projector: consumers must go through the query methods above,
     * which expose immutable {@link PromotionView} / {@link PromotionSummaryView} snapshots.</p>
     */
    private static final class PromotionState {
        private final String promotionId;
        private final String applicationId;
        private final String version;
        private final Environment sourceEnvironment;
        private final Environment targetEnvironment;
        private final String requestedBy;
        private final Instant requestedAt;
        private final List<PromotionHistoryEntry> history = new CopyOnWriteArrayList<>();

        private volatile PromotionStatus status;
        private volatile Instant lastUpdatedAt;

        private PromotionState(String promotionId,
                                String applicationId,
                                String version,
                                Environment sourceEnvironment,
                                Environment targetEnvironment,
                                String requestedBy,
                                Instant requestedAt) {
            this.promotionId = promotionId;
            this.applicationId = applicationId;
            this.version = version;
            this.sourceEnvironment = sourceEnvironment;
            this.targetEnvironment = targetEnvironment;
            this.requestedBy = requestedBy;
            this.requestedAt = requestedAt;
            this.status = PromotionStatus.REQUESTED;
            this.lastUpdatedAt = requestedAt;
        }

        private PromotionView toView() {
            return new PromotionView(
                    promotionId,
                    applicationId,
                    version,
                    sourceEnvironment,
                    targetEnvironment,
                    status,
                    requestedBy,
                    requestedAt,
                    List.copyOf(history)
            );
        }

        private PromotionSummaryView toSummary() {
            return new PromotionSummaryView(
                    promotionId,
                    version,
                    sourceEnvironment,
                    targetEnvironment,
                    status,
                    requestedAt,
                    lastUpdatedAt
            );
        }

        private EnvironmentStatusView toEnvironmentStatus() {
            return new EnvironmentStatusView(targetEnvironment, version, promotionId, status, lastUpdatedAt);
        }
    }
}
