package com.releasepilot.application.port;

/**
 * Port for sending notifications about Promotion lifecycle events to interested parties
 * (e.g., chat channels, email, etc.).
 */
public interface NotificationPort {

    /**
     * Sends a notification message related to the given Promotion.
     *
     * @param promotionId the identifier of the promotion this notification relates to
     * @param message     the notification message
     */
    void notify(String promotionId, String message);
}
