package com.releasepilot.domain.repository;

import com.releasepilot.domain.model.ApplicationId;
import com.releasepilot.domain.model.Environment;
import com.releasepilot.domain.model.Promotion;
import com.releasepilot.domain.model.PromotionId;
import com.releasepilot.domain.model.Version;

import java.util.Optional;

/**
 * Domain repository port for persisting and querying {@link Promotion} aggregates.
 *
 * <p>Implementations are responsible for providing the history checks required
 * to enforce the multi-promotion invariants declared in
 * {@link Promotion#request(ApplicationId, Version, Environment, Environment, boolean, boolean)}.</p>
 */
public interface PromotionRepository {

    /**
     * Persists the given Promotion.
     *
     * @param promotion the promotion to persist
     */
    void save(Promotion promotion);

    /**
     * Finds a Promotion by its identifier.
     *
     * @param id the promotion identifier
     * @return an {@link Optional} containing the Promotion if found, empty otherwise
     */
    Optional<Promotion> findById(PromotionId id);

    /**
     * Checks whether an active (non-terminal) Promotion already exists for the given
     * application and target environment.
     *
     * @param applicationId the application identifier
     * @param target        the target environment
     * @return true if an active promotion exists, false otherwise
     */
    boolean hasActivePromotion(ApplicationId applicationId, Environment target);

    /**
     * Checks whether the given version has already completed a promotion into the
     * given environment for the given application.
     *
     * @param applicationId the application identifier
     * @param version       the version being promoted
     * @param environment   the environment to check for completion
     * @return true if the version has completed the environment, false otherwise
     */
    boolean hasVersionCompletedEnvironment(ApplicationId applicationId, Version version, Environment environment);
}
