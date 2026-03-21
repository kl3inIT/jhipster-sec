package com.vn.core.security.access;

import com.vn.core.security.permission.EntityOp;

/**
 * Access context for CRUD-level checks on an entity.
 */
public class CrudEntityContext implements AccessContext {

    private final Class<?> entityClass;
    private final EntityOp operation;
    private boolean permitted = false;

    public CrudEntityContext(Class<?> entityClass, EntityOp operation) {
        this.entityClass = entityClass;
        this.operation = operation;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public EntityOp getOperation() {
        return operation;
    }

    public boolean isPermitted() {
        return permitted;
    }

    public void setPermitted(boolean permitted) {
        this.permitted = permitted;
    }
}
