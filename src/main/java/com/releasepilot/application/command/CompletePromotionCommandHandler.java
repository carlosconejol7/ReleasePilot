package com.releasepilot.application.command;

import com.releasepilot.application.event.PromotionEventPublisher;
import com.releasepilot.domain.event.PromotionEvent;
import com.releasepilot.domain.exception.DomainException;
import com.releasepilot.domain.model.Promotion;
import com.releasepilot.domain.model.PromotionId;
import com.releasepilot.domain.repository.PromotionRepository;
import org.springframework.stereotype.Service;

/**
 * Application service handling {@link CompletePromotionCommand}.
 *
 * <p>Loads the target {@link Promotion} aggregate, delegates the completion invariant
 * enforcement to it, and persists the resulting state via {@link PromotionRepository}.</p>
 */
@Service
public class CompletePromotionCommandHandler {

    private final PromotionRepository repository;
    private final PromotionEventPublisher publisher;

    public CompletePromotionCommandHandler(PromotionRepository repository, PromotionEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    /**
     * Handles the given {@link CompletePromotionCommand}, completing deployment for the referenced Promotion.
     *
     * @param command the command describing the completion request
     * @throws DomainException if the promotion does not exist, or if the transition invariants are violated
     */
    public void handle(CompletePromotionCommand command) {
        PromotionId promotionId = new PromotionId(command.promotionId());

        Promotion promotion = repository.findById(promotionId)
                .orElseThrow(() -> new DomainException("Promotion not found: " + command.promotionId()));

        promotion.completeDeployment(command.operator());

        repository.save(promotion);

        for (PromotionEvent event : promotion.pullDomainEvents()) {
            publisher.publish(event);
        }
    }
}
