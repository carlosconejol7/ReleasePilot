package com.releasepilot.infrastructure.api;

import com.releasepilot.query.model.PromotionView;
import com.releasepilot.query.service.PromotionQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing read-only Promotion detail endpoints.
 *
 * <p>Kept intentionally thin: all query logic lives in {@link PromotionQueryService}.</p>
 */
@RestController
@RequestMapping("/promotions")
public class PromotionController {

    private final PromotionQueryService promotionQueryService;

    public PromotionController(PromotionQueryService promotionQueryService) {
        this.promotionQueryService = promotionQueryService;
    }

    /**
     * Returns the detail view (including full state history) of a single Promotion.
     *
     * @param id the identifier of the promotion
     * @return {@code 200 OK} with the {@link PromotionView} if found, {@code 404 Not Found} otherwise
     */
    @GetMapping("/{id}")
    public ResponseEntity<PromotionView> getPromotion(@PathVariable String id) {
        return promotionQueryService.getPromotionById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
