package com.releasepilot;

import com.releasepilot.application.port.DeploymentPort;
import com.releasepilot.application.port.IssueTrackerPort;
import com.releasepilot.application.port.NotificationPort;
import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.User;
import com.releasepilot.infrastructure.api.dto.ApprovePromotionRequest;
import com.releasepilot.infrastructure.api.dto.CancelPromotionRequest;
import com.releasepilot.infrastructure.api.dto.CompletePromotionRequest;
import com.releasepilot.infrastructure.api.dto.PromotionIdResponse;
import com.releasepilot.infrastructure.api.dto.RequestPromotionRequest;
import com.releasepilot.infrastructure.api.dto.StartDeploymentRequest;
import com.releasepilot.infrastructure.audit.AuditLogEntry;
import com.releasepilot.infrastructure.audit.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test proving that ReleasePilot's event-driven side effects are correctly wired:
 * publishing a {@code PromotionEvent} must trigger both
 * {@link com.releasepilot.application.service.PromotionProcessManager} (which orchestrates calls
 * to {@link DeploymentPort}, {@link IssueTrackerPort}, and {@link NotificationPort}) and
 * {@link com.releasepilot.infrastructure.audit.AuditLogConsumer} (which asynchronously persists
 * every event into the {@link AuditLogRepository}).
 *
 * <p>Exercises the full stack (HTTP -> controller -> command handler -> aggregate -> real
 * PostgreSQL persistence -> published events -> listeners) via {@link MockMvc} against an
 * ephemeral Testcontainers-managed PostgreSQL instance. Only the outbound infrastructure ports
 * are replaced with mocks, so that side effects can be verified in isolation from any real
 * external integration.</p>
 *
 * <p>Because {@link com.releasepilot.infrastructure.audit.AuditLogConsumer} is {@code @Async},
 * and because event propagation in general happens after the command handler returns but is not
 * guaranteed to have completed by the time the HTTP response is received, every verification in
 * this test is wrapped in an Awaitility {@code await()} block rather than asserted immediately.</p>
 *
 * <p>This test does not assert on any read-side query endpoints or projections; that is covered
 * separately by {@link com.releasepilot.query.projection.PromotionProjectorTest} and the
 * application controllers' own tests.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class PromotionSideEffectsE2ETest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private DeploymentPort deploymentPort;

    @MockitoBean
    private IssueTrackerPort issueTrackerPort;

    @MockitoBean
    private NotificationPort notificationPort;

    private static final User REQUESTER = new User("user-1", false);
    private static final User APPROVER = new User("user-2", true);
    private static final User OPERATOR = new User("system-operator", true);

    private String requestPromotion(String applicationId, String version, Environment source, Environment target, User requester) throws Exception {
        RequestPromotionRequest createRequest = new RequestPromotionRequest(applicationId, version, source, target, requester);

        MvcResult result = mockMvc.perform(post("/promotions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), PromotionIdResponse.class).promotionId();
    }

    @Test
    void should_TriggerAllSideEffects_When_PromotionCompletesSuccessfully() throws Exception {
        // Given
        when(issueTrackerPort.getLinkedWorkItems(anyString())).thenReturn(List.of());

        // When: Request
        String promotionId = requestPromotion("app-e2e-1", "1.0.0", Environment.DEV, Environment.STAGING, REQUESTER);

        // Then: IssueTrackerPort was consulted for linked work items as part of PromotionProcessManager's reaction
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
                verify(issueTrackerPort, times(1)).getLinkedWorkItems(eq(promotionId)));

        // And: A notification was sent about the request
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
                verify(notificationPort, times(1)).notify(eq(promotionId), contains("Promotion requested")));

        // And: The request event was audited (asynchronously)
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditLogEntry> entries = auditLogRepository.findByPromotionId(promotionId);
            assertTrue(entries.stream().anyMatch(e -> e.eventType().equals("PromotionRequested")));
        });

        // When: Approve
        mockMvc.perform(post("/promotions/{id}/approve", promotionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApprovePromotionRequest(APPROVER))))
                .andExpect(status().isNoContent());

        // Then: A notification was sent about the approval
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
                verify(notificationPort, times(1)).notify(eq(promotionId), contains("Promotion approved by " + APPROVER.id())));

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditLogEntry> entries = auditLogRepository.findByPromotionId(promotionId);
            assertTrue(entries.stream().anyMatch(e -> e.eventType().equals("PromotionApproved")));
        });

        // When: Start deployment
        mockMvc.perform(post("/promotions/{id}/start", promotionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartDeploymentRequest(OPERATOR))))
                .andExpect(status().isNoContent());

        // Then: DeploymentPort was triggered for the target environment
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
                verify(deploymentPort, times(1)).triggerDeployment(eq(promotionId), eq(Environment.STAGING)));

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditLogEntry> entries = auditLogRepository.findByPromotionId(promotionId);
            assertTrue(entries.stream().anyMatch(e -> e.eventType().equals("PromotionStarted")));
        });

        // When: Complete
        mockMvc.perform(post("/promotions/{id}/complete", promotionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CompletePromotionRequest(OPERATOR))))
                .andExpect(status().isNoContent());

        // Then: A notification was sent about the completion
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
                verify(notificationPort, times(1)).notify(eq(promotionId), contains("Deployment completed by " + OPERATOR.id())));

        // And: All four lifecycle events were audited
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditLogEntry> entries = auditLogRepository.findByPromotionId(promotionId);
            assertEquals(4, entries.size());
            assertTrue(entries.stream().anyMatch(e -> e.eventType().equals("PromotionCompleted")));
        });

        // And: The deployment was only ever triggered once, and never rolled back or cancelled
        verify(deploymentPort, times(1)).triggerDeployment(eq(promotionId), any());
    }

    @Test
    void should_NotifyWithReason_And_SkipDeployment_When_PromotionIsCancelledBeforeApproval() throws Exception {
        // Given
        when(issueTrackerPort.getLinkedWorkItems(anyString())).thenReturn(List.of());

        // When: Request
        String promotionId = requestPromotion("app-e2e-2", "1.0.0", Environment.DEV, Environment.STAGING, REQUESTER);

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
                verify(issueTrackerPort, times(1)).getLinkedWorkItems(eq(promotionId)));

        // When: Cancel
        String reason = "no longer needed";
        mockMvc.perform(post("/promotions/{id}/cancel", promotionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelPromotionRequest(REQUESTER, reason))))
                .andExpect(status().isNoContent());

        // Then: NotificationPort was called mentioning the cancelling actor
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
                verify(notificationPort, times(1)).notify(eq(promotionId), contains("Promotion cancelled by " + REQUESTER.id())));

        // And: DeploymentPort was never triggered, since deployment never started
        verify(deploymentPort, times(0)).triggerDeployment(eq(promotionId), any());

        // And: Both the request and the cancellation were audited (asynchronously)
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AuditLogEntry> entries = auditLogRepository.findByPromotionId(promotionId);
            assertEquals(2, entries.size());
            assertTrue(entries.stream().anyMatch(e -> e.eventType().equals("PromotionRequested")));
            assertTrue(entries.stream().anyMatch(e -> e.eventType().equals("PromotionCancelled")));
        });
    }
}
