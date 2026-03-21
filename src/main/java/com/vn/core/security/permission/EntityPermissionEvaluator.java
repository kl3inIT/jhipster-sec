package com.vn.core.security.permission;

/**
 * Evaluates entity-level CRUD permissions for the current security context.
 */
public interface EntityPermissionEvaluator {
    /**
     * @return true if the current user is permitted to perform {@code op} on {@code entityClass}
     */
    boolean isPermitted(Class<?> entityClass, EntityOp op);
}
