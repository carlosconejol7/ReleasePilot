package com.releasepilot.infrastructure.readmodel;

import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.User;
import com.releasepilot.infrastructure.api.dto.ApprovePromotionRequest;
import com.releasepilot.infrastructure.api.dto.PromotionIdResponse;
import com.releasepilot.infrastructure.api.dto.RequestPromotionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Read-side integration test proving that {@code PromotionProjector} correctly translates
 * published {@code PromotionEvent}s into the API-facing read models exposed by
 * {@link com.releasepilot.infrastructure.api.PromotionController} and
 * {@link com.releasepilot.infrastructure.api.ApplicationController}.
 *
 * <p>This test deliberately focuses on the <b>read side only</b>: it drives state transitions
 * exclusively through the write-side HTTP endpoints (as a black box), and then asserts on the
 * exact JSON shape returned by the read-side query endpoints. It does not assert on write-side
 * persistence details (covered by {@code PromotionWriteIntegrationTest}), nor on process-manager
 * or audit-log side effects (covered by {@code PromotionSideEffectsE2ETest}).</p>
 *
 * <p>Because {@code PromotionProjector} is a synchronous {@code @EventListener} (running on the
 * same thread as the command handler, before the HTTP response is returned), every GET performed
 * immediately after a write MockMvc call is guaranteed to reflect that write with read-after-write
 * consistency. No {@code Awaitility} polling is required or used in this test.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class PromotionReadSideIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final User REQUESTER = new User("user-1", false);
    private static final User APPROVER = new User("user-2", true);

    @Test
    void should_ReflectRequestedThenApprovedState_When_QueryingReadModelsAfterEachWrite() throws Exception {
        String applicationId = "app-readmodel-1";
        String version = "1.0.0";

        // --- Write: request a promotion ---
        RequestPromotionRequest createRequest = new RequestPromotionRequest(
                applicationId, version, Environment.DEV, Environment.STAGING, REQUESTER);

        MvcResult createResult = mockMvc.perform(post("/promotions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String promotionId = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), PromotionIdResponse.class).promotionId();

        // --- Read: GET /promotions/{id} immediately reflects REQUESTED state ---
        mockMvc.perform(get("/promotions/{id}", promotionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promotionId").value(promotionId))
                .andExpect(jsonPath("$.applicationId").value(applicationId))
                .andExpect(jsonPath("$.version").value(version))
                .andExpect(jsonPath("$.sourceEnvironment").value("DEV"))
                .andExpect(jsonPath("$.targetEnvironment").value("STAGING"))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.requestedBy").value(REQUESTER.id()))
                .andExpect(jsonPath("$.history.length()").value(1))
                .andExpect(jsonPath("$.history[0].status").value("REQUESTED"))
                .andExpect(jsonPath("$.history[0].actor").value(REQUESTER.id()));

        // --- Read: GET /applications/{id}/status immediately reflects REQUESTED state ---
        mockMvc.perform(get("/applications/{id}/status", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(applicationId))
                .andExpect(jsonPath("$.environments.length()").value(1))
                .andExpect(jsonPath("$.environments[0].environment").value("STAGING"))
                .andExpect(jsonPath("$.environments[0].version").value(version))
                .andExpect(jsonPath("$.environments[0].promotionId").value(promotionId))
                .andExpect(jsonPath("$.environments[0].status").value("REQUESTED"));

        // --- Read: GET /applications/{id}/promotions immediately reflects REQUESTED state ---
        mockMvc.perform(get("/applications/{id}/promotions", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].promotionId").value(promotionId))
                .andExpect(jsonPath("$.items[0].status").value("REQUESTED"))
                .andExpect(jsonPath("$.items[0].sourceEnvironment").value("DEV"))
                .andExpect(jsonPath("$.items[0].targetEnvironment").value("STAGING"));

        // --- Write: approve the promotion ---
        ApprovePromotionRequest approveRequest = new ApprovePromotionRequest(APPROVER);

        mockMvc.perform(post("/promotions/{id}/approve", promotionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveRequest)))
                .andExpect(status().isNoContent());

        // --- Read: GET /promotions/{id} immediately reflects APPROVED state ---
        mockMvc.perform(get("/promotions/{id}", promotionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.history.length()").value(2))
                .andExpect(jsonPath("$.history[0].status").value("REQUESTED"))
                .andExpect(jsonPath("$.history[1].status").value("APPROVED"))
                .andExpect(jsonPath("$.history[1].actor").value(APPROVER.id()));

        // --- Read: GET /applications/{id}/status immediately reflects APPROVED state ---
        mockMvc.perform(get("/applications/{id}/status", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.environments.length()").value(1))
                .andExpect(jsonPath("$.environments[0].status").value("APPROVED"));

        // --- Read: GET /applications/{id}/promotions immediately reflects APPROVED state ---
        mockMvc.perform(get("/applications/{id}/promotions", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].status").value("APPROVED"));
    }

    @Test
    void should_ReturnNotFound_When_QueryingReadModelsForUnknownIdentifiers() throws Exception {
        mockMvc.perform(get("/promotions/{id}", "unknown-promotion-id"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/applications/{id}/status", "unknown-application-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_ReturnEmptyPagedResult_When_ApplicationHasNoPromotions() throws Exception {
        mockMvc.perform(get("/applications/{id}/promotions", "app-with-no-promotions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.items.length()").value(0));
    }
}
