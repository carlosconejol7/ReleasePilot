package com.releasepilot.application.command;

import com.releasepilot.application.event.DomainEventPublisher;
import com.releasepilot.domain.exception.DomainException;
import com.releasepilot.domain.model.Approver;
import com.releasepilot.domain.model.Promotion;
import com.releasepilot.domain.model.PromotionId;
import com.releasepilot.domain.repository.PromotionRepository;
import org.springframework.stereotype.Service;

/**
 * Application service handling {@link ApprovePromotionCommand}.
 *
 * <p>Loads the target {@link Promotion} aggregate, delegates the approval invariant
 * enforcement to it, and persists the resulting state via {@link PromotionRepository}.</p>
 */
@Service
public class ApprovePromotionCommandHandler {

    private final PromotionRepository repository;
    private final DomainEventPublisher publisher;

    public ApprovePromotionCommandHandler(PromotionRepository repository, DomainEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    /**
     * Handles the given {@link ApprovePromotionCommand}, approving the referenced Promotion.
     *
     * @param command the command describing the approval request
     * @throws DomainException if the promotion does not exist, or if the approval invariants are violated
     */
    public void handle(ApprovePromotionCommand command) {
        PromotionId promotionId = new PromotionId(command.promotionId());

        Promotion promotion = repository.findById(promotionId)
                .orElseThrow(() -> new DomainException("Promotion not found: " + command.promotionId()));

        promotion.approve(new Approver(command.approver()));

        repository.save(promotion);

        for (Object event : promotion.pullDomainEvents()) {
            publisher.publish(event);
        }
    }
}
