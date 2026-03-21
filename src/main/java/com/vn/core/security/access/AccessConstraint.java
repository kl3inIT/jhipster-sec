package com.vn.core.security.access;

/**
 * An access constraint that can modify a specific {@link AccessContext}
 * implementation (e.g. CRUD, attribute, row-level, fetch plan).
 */
public interface AccessConstraint<C extends AccessContext> {
    /**
     * @return the supported context type.
     */
    Class<C> supports();

    /**
     * Apply this constraint to the given context.
     */
    void applyTo(C context);

    /**
     * Order for applying multiple constraints of the same type.
     */
    default int getOrder() {
        return 0;
    }
}
