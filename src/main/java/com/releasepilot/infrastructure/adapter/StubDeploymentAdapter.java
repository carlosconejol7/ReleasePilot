package com.releasepilot.infrastructure.adapter;

import com.releasepilot.application.port.DeploymentPort;
import com.releasepilot.domain.model.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * In-memory stub implementation of {@link DeploymentPort}.
 *
 * <p>Performs no real deployment logic; simply logs the request. Intended as a
 * placeholder until a real deployment system integration is implemented.</p>
 */
@Component
public class StubDeploymentAdapter implements DeploymentPort {

    private static final Logger log = LoggerFactory.getLogger(StubDeploymentAdapter.class);

    @Override
    public void triggerDeployment(String promotionId, Environment target) {
        log.info("[StubDeploymentAdapter] Triggering deployment for promotion {} to environment {}", promotionId, target);
    }
}
