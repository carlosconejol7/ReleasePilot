package com.releasepilot.application.command;

import com.releasepilot.application.event.PromotionEventPublisher;
import com.releasepilot.domain.event.PromotionEvent;
import com.releasepilot.domain.exception.DomainException;
import com.releasepilot.domain.model.Promotion;
import com.releasepilot.domain.model.PromotionId;
import com.releasepilot.domain.repository.PromotionRepository;
import org.springframework.stereotype.Service;

/**
 * Application service handling {@link CancelPromotionCommand}.
 *
 * <p>Loads the target {@link Promotion} aggregate, delegates the cancellation invariant
 * enforcement to it, and persists the resulting state via {@link PromotionRepository}.</p>
 */
@Service
public class CancelPromotionCommandHandler {

    private final PromotionRepository repository;
    private final PromotionEventPublisher publisher;

    public CancelPromotionCommandHandler(PromotionRepository repository, PromotionEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    /**
     * Handles the given {@link CancelPromotionCommand}, cancelling the referenced Promotion.
     *
     * @param command the command describing the cancellation request
     * @throws DomainException if the promotion does not exist, or if the transition invariants are violated
     */
    public void handle(CancelPromotionCommand command) {
        PromotionId promotionId = new PromotionId(command.promotionId());

        Promotion promotion = repository.findById(promotionId)
                .orElseThrow(() -> new DomainException("Promotion not found: " + command.promotionId()));

        promotion.cancel(command.cancelledBy(), command.reason());

        repository.save(promotion);

        for (PromotionEvent event : promotion.pullDomainEvents()) {
            publisher.publish(event);
        }
    }
}
