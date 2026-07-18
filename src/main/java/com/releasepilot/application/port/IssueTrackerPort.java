package com.releasepilot.application.port;

import com.releasepilot.application.model.WorkItem;

import java.util.List;

/**
 * Port for retrieving work items linked to a Promotion from an external issue tracker system.
 */
public interface IssueTrackerPort {

    /**
     * Retrieves the work items linked to the given Promotion.
     *
     * @param promotionId the identifier of the promotion
     * @return the list of linked work items; empty if none are linked
     */
    List<WorkItem> getLinkedWorkItems(String promotionId);
}
