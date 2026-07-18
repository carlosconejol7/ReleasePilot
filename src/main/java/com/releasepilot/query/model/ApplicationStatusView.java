package com.releasepilot.query.model;

import java.util.List;

/**
 * Read model representing the current promotion state of an application across
 * all environments it has been promoted through.
 *
 * <p>This view is optimized for the {@code GET /applications/:id/status} endpoint.</p>
 *
 * @param applicationId the identifier of the application
 * @param environments   the current status of each environment the application has been promoted through,
 *                       ordered by environment progression
 */
public record ApplicationStatusView(
        String applicationId,
        List<EnvironmentStatusView> environments
) {
}
