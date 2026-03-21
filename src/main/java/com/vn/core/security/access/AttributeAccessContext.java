package com.vn.core.security.access;

/**
 * Access context for attribute-level checks on an entity.
 */
public class AttributeAccessContext implements AccessContext {

    private final Class<?> entityClass;
    private final String attribute;
    private final String action;
    private boolean permitted = false;

    public AttributeAccessContext(Class<?> entityClass, String attribute, String action) {
        this.entityClass = entityClass;
        this.attribute = attribute;
        this.action = action;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public String getAttribute() {
        return attribute;
    }

    public String getAction() {
        return action;
    }

    public boolean isPermitted() {
        return permitted;
    }

    public void setPermitted(boolean permitted) {
        this.permitted = permitted;
    }
}
