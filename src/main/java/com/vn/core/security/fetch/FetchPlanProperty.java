package com.vn.core.security.fetch;

/**
 * A single named property in a {@link FetchPlan}.
 * When {@code fetchPlan} is null the property is a scalar/local attribute.
 * When non-null it is a nested association with its own sub-plan.
 */
public record FetchPlanProperty(String name, FetchPlan fetchPlan, FetchMode fetchMode) {
    /**
     * Convenience constructor for scalar properties with default fetch mode.
     */
    public FetchPlanProperty(String name) {
        this(name, null, FetchMode.AUTO);
    }
}
