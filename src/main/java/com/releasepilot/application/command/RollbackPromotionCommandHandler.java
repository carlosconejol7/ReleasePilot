package com.releasepilot.application.command;

import com.releasepilot.application.event.DomainEventPublisher;
import com.releasepilot.domain.exception.DomainException;
import com.releasepilot.domain.model.Promotion;
import com.releasepilot.domain.model.PromotionId;
import com.releasepilot.domain.repository.PromotionRepository;
import org.springframework.stereotype.Service;

/**
 * Application service handling {@link RollbackPromotionCommand}.
 *
 * <p>Loads the target {@link Promotion} aggregate, delegates the rollback invariant
 * enforcement to it, and persists the resulting state via {@link PromotionRepository}.</p>
 */
@Service
public class RollbackPromotionCommandHandler {

    private final PromotionRepository repository;
    private final DomainEventPublisher publisher;

    public RollbackPromotionCommandHandler(PromotionRepository repository, DomainEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    /**
     * Handles the given {@link RollbackPromotionCommand}, rolling back the referenced Promotion.
     *
     * @param command the command describing the rollback request
     * @throws DomainException if the promotion does not exist, or if the transition invariants are violated
     */
    public void handle(RollbackPromotionCommand command) {
        PromotionId promotionId = new PromotionId(command.promotionId());

        Promotion promotion = repository.findById(promotionId)
                .orElseThrow(() -> new DomainException("Promotion not found: " + command.promotionId()));

        promotion.rollback(command.operator(), command.reason());

        repository.save(promotion);

        for (Object event : promotion.pullDomainEvents()) {
            publisher.publish(event);
        }
    }
}
