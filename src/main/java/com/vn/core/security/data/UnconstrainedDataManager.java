package com.vn.core.security.data;

import java.util.List;

/**
 * Trusted data access interface that bypasses security enforcement.
 * Use only from internal infrastructure code that has already enforced
 * its own access controls (e.g., row-policy evaluation, system tasks).
 */
public interface UnconstrainedDataManager {
    /**
     * Load an entity by class and id with no security checks.
     */
    <T> T load(Class<T> entityClass, Object id);

    /**
     * Load all entities of the given class with no security checks.
     */
    <T> List<T> loadAll(Class<T> entityClass);

    /**
     * Save an entity with no permission checks.
     */
    <T> T save(T entity);

    /**
     * Delete an entity by class and id with no permission checks.
     */
    void delete(Class<?> entityClass, Object id);
}
