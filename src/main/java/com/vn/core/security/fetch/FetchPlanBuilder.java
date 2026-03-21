package com.vn.core.security.fetch;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable builder for constructing {@link FetchPlan} instances in code.
 * Fetch plans built here are immutable once {@link #build()} is called.
 */
public class FetchPlanBuilder {

    private final Class<?> entityClass;
    private final String name;
    private final List<FetchPlanProperty> properties = new ArrayList<>();

    public FetchPlanBuilder(Class<?> entityClass, String name) {
        this.entityClass = entityClass;
        this.name = name;
    }

    /**
     * Adds a scalar property with default fetch mode.
     */
    public FetchPlanBuilder add(String propertyName) {
        properties.add(new FetchPlanProperty(propertyName));
        return this;
    }

    /**
     * Adds a nested association property referencing another plan by name.
     * The resolver will wire the plan reference on lookup.
     */
    public FetchPlanBuilder add(String propertyName, String nestedFetchPlanName) {
        // Create a placeholder FetchPlan with the given name — resolver wires it later
        FetchPlan placeholder = new FetchPlan(nestedFetchPlanName, null, List.of());
        properties.add(new FetchPlanProperty(propertyName, placeholder, FetchMode.AUTO));
        return this;
    }

    /**
     * Adds a nested association property with a fully resolved nested plan.
     */
    public FetchPlanBuilder add(String propertyName, FetchPlan nestedPlan) {
        properties.add(new FetchPlanProperty(propertyName, nestedPlan, FetchMode.AUTO));
        return this;
    }

    /**
     * Builds an immutable {@link FetchPlan} from the accumulated properties.
     */
    public FetchPlan build() {
        return new FetchPlan(name, entityClass, List.copyOf(properties));
    }
}
