package com.releasepilot.application.event;

/**
 * Port for publishing domain events to interested subscribers.
 */
public interface DomainEventPublisher {

    /**
     * Publishes the given domain event.
     *
     * @param event the domain event to publish
     */
    void publish(Object event);
}
