package com.releasepilot.query.service;

import com.releasepilot.query.model.ApplicationStatusView;
import com.releasepilot.query.projection.PromotionProjector;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Query service exposing an application's per-environment promotion status to the API layer.
 *
 * <p>Contains no business logic; it delegates directly to the in-memory
 * {@link PromotionProjector} read repository.</p>
 */
@Service
public class ApplicationStatusQueryService {

    private final PromotionProjector projector;

    public ApplicationStatusQueryService(PromotionProjector projector) {
        this.projector = projector;
    }

    /**
     * Retrieves the current per-environment status for a given application.
     *
     * @param applicationId the identifier of the application
     * @return an {@link Optional} containing the {@link ApplicationStatusView} if known, empty otherwise
     */
    public Optional<ApplicationStatusView> getApplicationStatus(String applicationId) {
        return projector.findApplicationStatus(applicationId);
    }
}
