package com.releasepilot.infrastructure.audit;

import com.releasepilot.domain.event.PromotionApproved;
import com.releasepilot.domain.event.PromotionCancelled;
import com.releasepilot.domain.event.PromotionCompleted;
import com.releasepilot.domain.event.PromotionRequested;
import com.releasepilot.domain.event.PromotionRolledBack;
import com.releasepilot.domain.event.PromotionStarted;
import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link AuditLogConsumer}, verifying that each {@link com.releasepilot.domain.event.PromotionEvent}
 * type is correctly translated into an {@link AuditLogEntry} and persisted.
 *
 * <p>The consumer is invoked directly (synchronously) here; the {@code @Async} behavior itself
 * is a Spring proxying concern and is intentionally not re-verified by these focused unit tests.</p>
 */
class AuditLogConsumerTest {

    private static final User REQUESTER = new User("user-1", false);
    private static final User APPROVER = new User("user-2", true);
    private static final User OPERATOR = new User("system-operator", true);

    @Test
    void should_PersistAuditLogEntry_When_PromotionRequestedEventReceived() {
        // Given
        AuditLogRepository repository = new AuditLogRepository();
        AuditLogConsumer consumer = new AuditLogConsumer(repository);
        PromotionRequested event = new PromotionRequested("promotion-1", "app-1", "1.0.0", Environment.DEV, Environment.STAGING, REQUESTER, Instant.now());

        // When
        consumer.on(event);

        // Then
        List<AuditLogEntry> entries = repository.findByPromotionId("promotion-1");
        assertEquals(1, entries.size());
        assertEquals("PromotionRequested", entries.get(0).eventType());
        assertEquals("promotion-1", entries.get(0).promotionId());
        assertEquals("user-1", entries.get(0).actingUser());
    }

    @Test
    void should_PersistAuditLogEntry_When_PromotionApprovedEventReceived() {
        // Given
        AuditLogRepository repository = new AuditLogRepository();
        AuditLogConsumer consumer = new AuditLogConsumer(repository);
        PromotionApproved event = new PromotionApproved("promotion-1", APPROVER, Instant.now());

        // When
        consumer.on(event);

        // Then
        List<AuditLogEntry> entries = repository.findByPromotionId("promotion-1");
        assertEquals(1, entries.size());
        assertEquals("PromotionApproved", entries.get(0).eventType());
        assertEquals("user-2", entries.get(0).actingUser());
    }

    @Test
    void should_PersistAuditLogEntry_When_PromotionStartedEventReceived() {
        // Given
        AuditLogRepository repository = new AuditLogRepository();
        AuditLogConsumer consumer = new AuditLogConsumer(repository);
        PromotionStarted event = new PromotionStarted("promotion-1", OPERATOR, Instant.now());

        // When
        consumer.on(event);

        // Then
        List<AuditLogEntry> entries = repository.findByPromotionId("promotion-1");
        assertEquals(1, entries.size());
        assertEquals("PromotionStarted", entries.get(0).eventType());
        assertEquals("system-operator", entries.get(0).actingUser());
    }

    @Test
    void should_PersistAuditLogEntry_When_PromotionCompletedEventReceived() {
        // Given
        AuditLogRepository repository = new AuditLogRepository();
        AuditLogConsumer consumer = new AuditLogConsumer(repository);
        PromotionCompleted event = new PromotionCompleted("promotion-1", OPERATOR, Instant.now());

        // When
        consumer.on(event);

        // Then
        List<AuditLogEntry> entries = repository.findByPromotionId("promotion-1");
        assertEquals(1, entries.size());
        assertEquals("PromotionCompleted", entries.get(0).eventType());
    }

    @Test
    void should_PersistAuditLogEntry_When_PromotionCancelledEventReceived() {
        // Given
        AuditLogRepository repository = new AuditLogRepository();
        AuditLogConsumer consumer = new AuditLogConsumer(repository);
        PromotionCancelled event = new PromotionCancelled("promotion-1", REQUESTER, "no longer needed", Instant.now());

        // When
        consumer.on(event);

        // Then
        List<AuditLogEntry> entries = repository.findByPromotionId("promotion-1");
        assertEquals(1, entries.size());
        assertEquals("PromotionCancelled", entries.get(0).eventType());
    }

    @Test
    void should_PersistAuditLogEntry_When_PromotionRolledBackEventReceived() {
        // Given
        AuditLogRepository repository = new AuditLogRepository();
        AuditLogConsumer consumer = new AuditLogConsumer(repository);
        PromotionRolledBack event = new PromotionRolledBack("promotion-1", OPERATOR, "deployment failed", Instant.now());

        // When
        consumer.on(event);

        // Then
        List<AuditLogEntry> entries = repository.findByPromotionId("promotion-1");
        assertEquals(1, entries.size());
        assertEquals("PromotionRolledBack", entries.get(0).eventType());
    }

    @Test
    void should_AccumulateMultipleEntries_When_MultipleEventsReceivedForSamePromotion() {
        // Given
        AuditLogRepository repository = new AuditLogRepository();
        AuditLogConsumer consumer = new AuditLogConsumer(repository);

        // When
        consumer.on(new PromotionRequested("promotion-1", "app-1", "1.0.0", Environment.DEV, Environment.STAGING, REQUESTER, Instant.now()));
        consumer.on(new PromotionApproved("promotion-1", APPROVER, Instant.now()));

        // Then
        List<AuditLogEntry> entries = repository.findByPromotionId("promotion-1");
        assertEquals(2, entries.size());
    }

    @Test
    void should_KeepEntriesIsolatedPerPromotion_When_EventsForDifferentPromotionsAreReceived() {
        // Given
        AuditLogRepository repository = new AuditLogRepository();
        AuditLogConsumer consumer = new AuditLogConsumer(repository);

        // When
        consumer.on(new PromotionRequested("promotion-1", "app-1", "1.0.0", Environment.DEV, Environment.STAGING, REQUESTER, Instant.now()));
        consumer.on(new PromotionRequested("promotion-2", "app-1", "1.1.0", Environment.DEV, Environment.STAGING, REQUESTER, Instant.now()));

        // Then
        assertEquals(1, repository.findByPromotionId("promotion-1").size());
        assertEquals(1, repository.findByPromotionId("promotion-2").size());
        assertEquals(2, repository.findAll().size());
    }
}
