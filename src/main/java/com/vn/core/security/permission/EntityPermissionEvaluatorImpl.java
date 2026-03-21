package com.vn.core.security.permission;

import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link EntityPermissionEvaluator} that delegates
 * to {@link RolePermissionService} for entity-level CRUD permission checks.
 */
@Component
public class EntityPermissionEvaluatorImpl implements EntityPermissionEvaluator {

    private final RolePermissionService rolePermissionService;

    public EntityPermissionEvaluatorImpl(RolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    @Override
    public boolean isPermitted(Class<?> entityClass, EntityOp op) {
        return rolePermissionService.isEntityOpPermitted(entityClass, op);
    }
}
