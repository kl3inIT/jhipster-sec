package com.vn.core.security.access;

/**
 * Pipeline that applies all registered {@link AccessConstraint}s to an {@link AccessContext}.
 */
public interface AccessManager {
    /**
     * Apply all registered constraints that support the given context type.
     *
     * @param context the access context to evaluate
     * @param <C>     the context type
     * @return the mutated context after all matching constraints have been applied
     */
    <C extends AccessContext> C applyRegisteredConstraints(C context);
}
