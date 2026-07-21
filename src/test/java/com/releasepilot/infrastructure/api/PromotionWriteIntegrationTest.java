package com.releasepilot.infrastructure.api;

import com.releasepilot.AbstractIntegrationTest;
import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.Promotion;
import com.releasepilot.domain.model.PromotionId;
import com.releasepilot.domain.model.PromotionStatus;
import com.releasepilot.domain.model.User;
import com.releasepilot.domain.repository.PromotionRepository;
import com.releasepilot.infrastructure.api.dto.ApprovePromotionRequest;
import com.releasepilot.infrastructure.api.dto.PromotionIdResponse;
import com.releasepilot.infrastructure.api.dto.RequestPromotionRequest;
import com.releasepilot.infrastructure.api.dto.StartDeploymentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Write-side integration tests for {@link PromotionController}, exercising the full stack
 * (HTTP -> controller -> command handler -> aggregate -> real PostgreSQL persistence) via
 * {@link MockMvc} and an ephemeral Testcontainers-managed PostgreSQL instance.
 *
 * <p>Each test both asserts on the HTTP response and independently verifies the resulting
 * database state through the injected {@link PromotionRepository} port, proving the write
 * side is correctly wired end-to-end against a real database rather than an in-memory stub.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class PromotionWriteIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PromotionRepository promotionRepository;

    @Test
    void shouldCreatePromotionSuccessfully() throws Exception {
        // Given
        User requester = new User("user-1", false);
        RequestPromotionRequest request = new RequestPromotionRequest(
                "app-create-1", "1.0.0", Environment.DEV, Environment.STAGING, requester);

        // When
        MvcResult result = mockMvc.perform(post("/promotions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        // Then
        PromotionIdResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), PromotionIdResponse.class);

        Optional<Promotion> saved = promotionRepository.findById(new PromotionId(response.promotionId()));
        assertTrue(saved.isPresent());
        assertEquals(PromotionStatus.REQUESTED, saved.get().getStatus());
        assertEquals("app-create-1", saved.get().getApplicationId().value());
        assertEquals("1.0.0", saved.get().getVersion().value());
        assertEquals(Environment.DEV, saved.get().getSourceEnvironment());
        assertEquals(Environment.STAGING, saved.get().getTargetEnvironment());
    }

    @Test
    void shouldApprovePromotion() throws Exception {
        // Given
        User requester = new User("user-2", false);
        User approver = new User("user-3", true);
        RequestPromotionRequest createRequest = new RequestPromotionRequest(
                "app-approve-1", "1.0.0", Environment.DEV, Environment.STAGING, requester);

        MvcResult createResult = mockMvc.perform(post("/promotions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String promotionId = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), PromotionIdResponse.class).promotionId();

        ApprovePromotionRequest approveRequest = new ApprovePromotionRequest(approver);

        // When
        mockMvc.perform(post("/promotions/{id}/approve", promotionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveRequest)))
                .andExpect(status().isNoContent());

        // Then
        Optional<Promotion> saved = promotionRepository.findById(new PromotionId(promotionId));
        assertTrue(saved.isPresent());
        assertEquals(PromotionStatus.APPROVED, saved.get().getStatus());
    }

    @Test
    void shouldRejectInvalidTransition() throws Exception {
        // Given
        User requester = new User("user-4", false);
        User operator = new User("system-operator", true);
        RequestPromotionRequest createRequest = new RequestPromotionRequest(
                "app-invalid-1", "1.0.0", Environment.DEV, Environment.STAGING, requester);

        MvcResult createResult = mockMvc.perform(post("/promotions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String promotionId = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), PromotionIdResponse.class).promotionId();

        // The promotion is still REQUESTED; starting deployment before approval is an illegal transition.
        StartDeploymentRequest startRequest = new StartDeploymentRequest(operator);

        // When
        mockMvc.perform(post("/promotions/{id}/start", promotionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startRequest)))
                .andExpect(status().isBadRequest());

        // Then
        Optional<Promotion> saved = promotionRepository.findById(new PromotionId(promotionId));
        assertTrue(saved.isPresent());
        assertEquals(PromotionStatus.REQUESTED, saved.get().getStatus());
    }
}
