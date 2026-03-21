package com.vn.core.security.fetch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link FetchPlanResolver}.
 * Resolves fetch plans via the registered {@link FetchPlanRepository},
 * throwing {@link IllegalArgumentException} if the plan is not found.
 */
@Component
public class FetchPlanResolverImpl implements FetchPlanResolver {

    private static final Logger LOG = LoggerFactory.getLogger(FetchPlanResolverImpl.class);

    private final FetchPlanRepository fetchPlanRepository;

    public FetchPlanResolverImpl(FetchPlanRepository fetchPlanRepository) {
        this.fetchPlanRepository = fetchPlanRepository;
    }

    @Override
    public FetchPlan resolve(Class<?> entityClass, String fetchPlanCode) {
        LOG.debug("Resolving fetch plan '{}' for entity {}", fetchPlanCode, entityClass.getSimpleName());
        return fetchPlanRepository
            .findByEntityAndName(entityClass, fetchPlanCode)
            .orElseThrow(() ->
                new IllegalArgumentException("No fetch plan '" + fetchPlanCode + "' for " + entityClass.getSimpleName())
            );
    }
}
