package com.releasepilot.query.service;

import com.releasepilot.query.model.PagedResult;
import com.releasepilot.query.model.PromotionSummaryView;
import com.releasepilot.query.model.PromotionView;
import com.releasepilot.query.projection.PromotionProjector;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Query service exposing Promotion read models to the API layer.
 *
 * <p>Contains no business logic; it delegates directly to the in-memory
 * {@link PromotionProjector} read repository.</p>
 */
@Service
public class PromotionQueryService {

    private final PromotionProjector projector;

    public PromotionQueryService(PromotionProjector projector) {
        this.projector = projector;
    }

    /**
     * Retrieves the detail view of a single Promotion.
     *
     * @param promotionId the identifier of the promotion
     * @return an {@link Optional} containing the {@link PromotionView} if found, empty otherwise
     */
    public Optional<PromotionView> getPromotionById(String promotionId) {
        return projector.findById(promotionId);
    }

    /**
     * Retrieves a page of the promotion history for a given application.
     *
     * @param applicationId the identifier of the application
     * @param page          the zero-based page index
     * @param size          the maximum number of items per page
     * @return a {@link PagedResult} of {@link PromotionSummaryView} items
     */
    public PagedResult<PromotionSummaryView> getPromotionHistoryForApplication(String applicationId, int page, int size) {
        return projector.findPromotionsByApplication(applicationId, page, size);
    }
}
