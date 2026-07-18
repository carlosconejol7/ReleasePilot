package com.releasepilot.application.port;

import com.releasepilot.domain.model.Environment;

/**
 * Port for triggering the deployment of a Promotion to its target Environment.
 *
 * <p>Implementations are responsible for integrating with whatever deployment
 * system is used (e.g., a CI/CD pipeline, orchestration tool, etc.).</p>
 */
public interface DeploymentPort {

    /**
     * Triggers the deployment of the given Promotion to the given target Environment.
     *
     * @param promotionId the identifier of the promotion being deployed
     * @param target      the target environment to deploy to
     */
    void triggerDeployment(String promotionId, Environment target);
}
