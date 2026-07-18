package com.releasepilot.infrastructure.audit;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory repository for {@link AuditLogEntry} records.
 *
 * <p>Backed by a {@link CopyOnWriteArrayList} to safely support concurrent writes from
 * asynchronous audit consumers alongside reads from any future query/reporting layer.</p>
 */
@Repository
public class AuditLogRepository {

    private final List<AuditLogEntry> entries = new CopyOnWriteArrayList<>();

    /**
     * Persists the given audit log entry.
     *
     * @param entry the entry to persist
     */
    public void save(AuditLogEntry entry) {
        entries.add(entry);
    }

    /**
     * Returns all audit log entries recorded so far, in insertion order.
     *
     * @return an immutable snapshot of all recorded entries
     */
    public List<AuditLogEntry> findAll() {
        return List.copyOf(entries);
    }

    /**
     * Returns all audit log entries recorded for the given promotion, in insertion order.
     *
     * @param promotionId the identifier of the promotion
     * @return an immutable snapshot of the matching entries
     */
    public List<AuditLogEntry> findByPromotionId(String promotionId) {
        return entries.stream()
                .filter(entry -> entry.promotionId().equals(promotionId))
                .toList();
    }
}
