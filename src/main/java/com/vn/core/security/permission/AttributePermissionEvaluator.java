package com.vn.core.security.permission;

/**
 * Evaluates attribute-level view and edit permissions for the current security context.
 */
public interface AttributePermissionEvaluator {
    /**
     * @return true if the current user may view the given attribute on the entity class
     */
    boolean canView(Class<?> entityClass, String attribute);

    /**
     * @return true if the current user may edit the given attribute on the entity class
     */
    boolean canEdit(Class<?> entityClass, String attribute);
}
