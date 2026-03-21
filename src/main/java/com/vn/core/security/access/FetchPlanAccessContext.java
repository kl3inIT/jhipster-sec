package com.vn.core.security.access;

/**
 * Access context for applying a particular fetch plan on an entity.
 * Defaults to permitted — the catalog allowlist is the primary gate.
 */
public class FetchPlanAccessContext implements AccessContext {

    private final Class<?> entityClass;
    private final String fetchPlanCode;
    private boolean permitted = true;

    public FetchPlanAccessContext(Class<?> entityClass, String fetchPlanCode) {
        this.entityClass = entityClass;
        this.fetchPlanCode = fetchPlanCode;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public String getFetchPlanCode() {
        return fetchPlanCode;
    }

    public boolean isPermitted() {
        return permitted;
    }

    public void setPermitted(boolean permitted) {
        this.permitted = permitted;
    }
}
