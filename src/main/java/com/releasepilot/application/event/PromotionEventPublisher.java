package com.releasepilot.application.event;

import com.releasepilot.domain.event.PromotionEvent;

/**
 * Port for publishing Promotion domain events to interested subscribers.
 */
public interface PromotionEventPublisher {

    /**
     * Publishes the given Promotion domain event.
     *
     * @param event the domain event to publish
     */
    void publish(PromotionEvent event);
}
