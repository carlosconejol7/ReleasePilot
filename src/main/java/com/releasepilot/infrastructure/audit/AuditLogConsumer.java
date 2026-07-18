package com.releasepilot.infrastructure.audit;

import com.releasepilot.domain.event.PromotionEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Infrastructure consumer that records every {@link PromotionEvent} into the audit trail.
 *
 * <p>Runs asynchronously (via {@link Async}) relative to the thread that published the event,
 * so that the command/API request that triggered the event is never delayed waiting for
 * audit logging to complete. This proves the system is fully event-driven: the write side
 * publishes an event and moves on, while any number of independent listeners — this one
 * included — react to it on their own time.</p>
 *
 * <p>Requires {@code @EnableAsync} to be active in the application context (see
 * {@code com.releasepilot.infrastructure.config.AsyncConfig}) for the {@link Async} annotation
 * to actually take effect.</p>
 */
@Component
public class AuditLogConsumer {

    private final AuditLogRepository auditLogRepository;

    public AuditLogConsumer(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Handles an incoming {@link PromotionEvent} by persisting an {@link AuditLogEntry}
     * derived from its common fields.
     *
     * @param event the domain event to audit
     */
    @Async
    @EventListener
    public void on(PromotionEvent event) {
        AuditLogEntry entry = new AuditLogEntry(
                event.getClass().getSimpleName(),
                event.promotionId(),
                event.occurredAt(),
                event.actor().id()
        );
        auditLogRepository.save(entry);
    }
}
