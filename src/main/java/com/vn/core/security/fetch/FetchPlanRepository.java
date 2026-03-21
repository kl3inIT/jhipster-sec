package com.vn.core.security.fetch;

import java.util.List;
import java.util.Optional;

/**
 * Repository for locating named {@link FetchPlan}s by entity class.
 * Implementations read from YAML files or code-based registrations only.
 */
public interface FetchPlanRepository {
    /**
     * Find a fetch plan by entity class and plan name.
     */
    Optional<FetchPlan> findByEntityAndName(Class<?> entityClass, String name);

    /**
     * Return all plans registered for the given entity class.
     */
    List<FetchPlan> findAllByEntity(Class<?> entityClass);
}
