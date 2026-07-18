package com.releasepilot.infrastructure.api;

import com.releasepilot.query.model.ApplicationStatusView;
import com.releasepilot.query.model.PagedResult;
import com.releasepilot.query.model.PromotionSummaryView;
import com.releasepilot.query.service.ApplicationStatusQueryService;
import com.releasepilot.query.service.PromotionQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing read-only, application-scoped Promotion endpoints.
 *
 * <p>Kept intentionally thin: all query logic lives in {@link ApplicationStatusQueryService}
 * and {@link PromotionQueryService}.</p>
 */
@RestController
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationStatusQueryService applicationStatusQueryService;
    private final PromotionQueryService promotionQueryService;

    public ApplicationController(ApplicationStatusQueryService applicationStatusQueryService,
                                  PromotionQueryService promotionQueryService) {
        this.applicationStatusQueryService = applicationStatusQueryService;
        this.promotionQueryService = promotionQueryService;
    }

    /**
     * Returns the current per-environment promotion status for a given application.
     *
     * @param id the identifier of the application
     * @return {@code 200 OK} with the {@link ApplicationStatusView} if known, {@code 404 Not Found} otherwise
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<ApplicationStatusView> getStatus(@PathVariable String id) {
        return applicationStatusQueryService.getApplicationStatus(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Returns a page of the promotion history for a given application, most recently
     * requested first.
     *
     * @param id   the identifier of the application
     * @param page the zero-based page index (defaults to {@code 0})
     * @param size the maximum number of items per page (defaults to {@code 20})
     * @return {@code 200 OK} with a {@link PagedResult} of {@link PromotionSummaryView} items
     */
    @GetMapping("/{id}/promotions")
    public ResponseEntity<PagedResult<PromotionSummaryView>> getPromotions(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(promotionQueryService.getPromotionHistoryForApplication(id, page, size));
    }
}
