package com.vn.core.security.fetch;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Immutable definition of which properties to load for an entity type.
 * Fetch plans are defined in YAML or code builders — never in the database.
 */
public class FetchPlan {

    private final String name;
    private final Class<?> entityClass;
    private final List<FetchPlanProperty> properties;

    public FetchPlan(String name, Class<?> entityClass, List<FetchPlanProperty> properties) {
        this.name = name;
        this.entityClass = entityClass;
        this.properties = Collections.unmodifiableList(properties);
    }

    public String getName() {
        return name;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public List<FetchPlanProperty> getProperties() {
        return properties;
    }

    public Optional<FetchPlanProperty> getProperty(String propertyName) {
        return properties
            .stream()
            .filter(p -> p.name().equals(propertyName))
            .findFirst();
    }
}
