package com.releasepilot.application.command;

import com.releasepilot.application.event.DomainEventPublisher;
import com.releasepilot.domain.model.ApplicationId;
import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.Promotion;
import com.releasepilot.domain.model.PromotionId;
import com.releasepilot.domain.model.Version;
import com.releasepilot.domain.repository.PromotionRepository;
import org.springframework.stereotype.Service;

/**
 * Application service handling {@link RequestPromotionCommand}.
 *
 * <p>Orchestrates the retrieval of the history checks required by the domain,
 * delegates the invariant enforcement to the {@link Promotion} aggregate, and
 * persists the resulting Promotion via {@link PromotionRepository}.</p>
 */
@Service
public class RequestPromotionCommandHandler {

    private final PromotionRepository repository;
    private final DomainEventPublisher publisher;

    public RequestPromotionCommandHandler(PromotionRepository repository, DomainEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    /**
     * Handles the given {@link RequestPromotionCommand}, creating and persisting a new
     * {@link Promotion} if all domain invariants are satisfied.
     *
     * @param command the command describing the requested promotion
     * @return the identifier of the newly created Promotion
     */
    public PromotionId handle(RequestPromotionCommand command) {
        ApplicationId applicationId = new ApplicationId(command.applicationId());
        Version version = new Version(command.version());
        Environment source = command.sourceEnvironment();
        Environment target = command.targetEnvironment();

        boolean hasActivePromotionInTarget = repository.hasActivePromotion(applicationId, target);
        boolean hasCompletedPreviousEnvironment = source == Environment.DEV
                || repository.hasVersionCompletedEnvironment(applicationId, version, source);

        Promotion promotion = Promotion.request(
                applicationId,
                version,
                source,
                target,
                command.requestedBy(),
                hasCompletedPreviousEnvironment,
                hasActivePromotionInTarget
        );

        repository.save(promotion);

        for (Object event : promotion.pullDomainEvents()) {
            publisher.publish(event);
        }

        return promotion.getId();
    }
}
