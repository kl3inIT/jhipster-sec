package com.vn.core.security.permission;

import java.util.Collection;

/**
 * Abstraction for dynamic permission lookup against the current role model.
 * Callers work against this interface; the database implementation is provided in a later plan.
 */
public interface RolePermissionService {
    /**
     * Checks whether the current authenticated user is allowed to perform the given
     * CRUD operation on the entity class.
     */
    boolean isEntityOpPermitted(Class<?> entityClass, EntityOp op);

    /**
     * Checks if any of the given authorities has the specified permission.
     *
     * @param authorityNames authorities of the current user (e.g. Spring Security roles)
     * @param targetType     type of secured resource
     * @param target         logical target code (e.g. ORDER, ORDER.totalAmount)
     * @param action         action on the target (e.g. READ, EDIT, APPLY)
     */
    boolean hasPermission(Collection<String> authorityNames, TargetType targetType, String target, String action);
}
