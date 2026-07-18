package com.releasepilot.application.model;

/**
 * Represents a work item (e.g., a ticket or issue) linked to a Promotion,
 * as reported by an external issue tracker system.
 *
 * @param id    the identifier of the work item in the issue tracker
 * @param title the title/summary of the work item
 * @param url   a link to the work item in the issue tracker
 */
public record WorkItem(String id, String title, String url) {
}
