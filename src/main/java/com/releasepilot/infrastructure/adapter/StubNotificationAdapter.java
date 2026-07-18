package com.releasepilot.infrastructure.adapter;

import com.releasepilot.application.port.NotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * In-memory stub implementation of {@link NotificationPort}.
 *
 * <p>Performs no real notification delivery; simply logs the message. Intended as a
 * placeholder until a real notification channel (e.g., chat, email) is implemented.</p>
 */
@Component
public class StubNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(StubNotificationAdapter.class);

    @Override
    public void notify(String promotionId, String message) {
        log.info("[StubNotificationAdapter] Promotion {}: {}", promotionId, message);
    }
}
