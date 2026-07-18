package com.releasepilot.application.command;

import com.releasepilot.application.event.PromotionEventPublisher;
import com.releasepilot.domain.event.PromotionEvent;
import com.releasepilot.domain.exception.DomainException;
import com.releasepilot.domain.model.Promotion;
import com.releasepilot.domain.model.PromotionId;
import com.releasepilot.domain.repository.PromotionRepository;
import org.springframework.stereotype.Service;

/**
 * Application service handling {@link StartDeploymentCommand}.
 *
 * <p>Loads the target {@link Promotion} aggregate, delegates the deployment-start invariant
 * enforcement to it, and persists the resulting state via {@link PromotionRepository}.</p>
 */
@Service
public class StartDeploymentCommandHandler {

    private final PromotionRepository repository;
    private final PromotionEventPublisher publisher;

    public StartDeploymentCommandHandler(PromotionRepository repository, PromotionEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    /**
     * Handles the given {@link StartDeploymentCommand}, starting deployment for the referenced Promotion.
     *
     * @param command the command describing the deployment-start request
     * @throws DomainException if the promotion does not exist, or if the transition invariants are violated
     */
    public void handle(StartDeploymentCommand command) {
        PromotionId promotionId = new PromotionId(command.promotionId());

        Promotion promotion = repository.findById(promotionId)
                .orElseThrow(() -> new DomainException("Promotion not found: " + command.promotionId()));

        promotion.startDeployment(command.operator());

        repository.save(promotion);

        for (PromotionEvent event : promotion.pullDomainEvents()) {
            publisher.publish(event);
        }
    }
}
