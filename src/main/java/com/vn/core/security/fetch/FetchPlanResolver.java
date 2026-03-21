package com.vn.core.security.fetch;

/**
 * Resolves a {@link FetchPlan} by entity class and plan code, throwing if not found.
 */
public interface FetchPlanResolver {
    /**
     * Resolve a fetch plan, throwing if the plan code is unknown for this entity.
     *
     * @param entityClass  the entity type
     * @param fetchPlanCode the logical plan code
     * @return the resolved plan
     * @throws IllegalArgumentException if no plan exists for the given class and code
     */
    FetchPlan resolve(Class<?> entityClass, String fetchPlanCode);
}
