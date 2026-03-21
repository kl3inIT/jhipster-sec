package com.vn.core.security.fetch;

import org.springframework.stereotype.Component;

/**
 * Factory component for creating {@link FetchPlanBuilder} instances in code.
 * Use this bean to define fetch plans programmatically.
 */
@Component
public class FetchPlans {

    /**
     * Creates a new builder for the given entity class and plan name.
     *
     * @param entityClass the entity type this plan applies to
     * @param name        the logical name / code of the fetch plan
     * @return a new mutable builder
     */
    public FetchPlanBuilder builder(Class<?> entityClass, String name) {
        return new FetchPlanBuilder(entityClass, name);
    }
}
