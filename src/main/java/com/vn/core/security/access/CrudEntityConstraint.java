package com.vn.core.security.access;

import com.vn.core.security.permission.RolePermissionService;
import org.springframework.stereotype.Component;

/**
 * Applies CRUD entity permissions to {@link CrudEntityContext} using {@link RolePermissionService}.
 */
@Component
public class CrudEntityConstraint implements AccessConstraint<CrudEntityContext> {

    private final RolePermissionService rolePermissionService;

    public CrudEntityConstraint(RolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    @Override
    public Class<CrudEntityContext> supports() {
        return CrudEntityContext.class;
    }

    @Override
    public void applyTo(CrudEntityContext ctx) {
        ctx.setPermitted(rolePermissionService.isEntityOpPermitted(ctx.getEntityClass(), ctx.getOperation()));
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
