package com.releasepilot.infrastructure.event;

import com.releasepilot.application.event.PromotionEventPublisher;
import com.releasepilot.domain.event.PromotionEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Infrastructure adapter implementing the {@link PromotionEventPublisher} port on top of Spring's
 * {@link ApplicationEventPublisher}.
 *
 * <p>This decouples the write side (which only knows about the {@link PromotionEventPublisher}
 * port) from the read side: any number of listeners (such as {@code PromotionProjector}) may
 * subscribe to these events via {@code @EventListener} without the write side being aware of
 * them.</p>
 */
@Component
public class SpringPromotionEventPublisher implements PromotionEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringPromotionEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(PromotionEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
