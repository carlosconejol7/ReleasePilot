package com.releasepilot.infrastructure.adapter;

import com.releasepilot.application.model.WorkItem;
import com.releasepilot.application.port.IssueTrackerPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * In-memory stub implementation of {@link IssueTrackerPort}.
 *
 * <p>Performs no real issue tracker integration; simply logs the request and returns
 * an empty list. Intended as a placeholder until a real issue tracker integration
 * is implemented.</p>
 */
@Component
public class StubIssueTrackerAdapter implements IssueTrackerPort {

    private static final Logger log = LoggerFactory.getLogger(StubIssueTrackerAdapter.class);

    @Override
    public List<WorkItem> getLinkedWorkItems(String promotionId) {
        log.info("[StubIssueTrackerAdapter] Fetching linked work items for promotion {}", promotionId);
        return List.of();
    }
}
