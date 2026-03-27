package com.vn.core.security.data;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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
     * Load one entity matching the given specification with no security checks.
     */
    <T> Optional<T> loadOne(Class<T> entityClass, Specification<T> spec);

    /**
     * Load all entities matching the given specification with no security checks.
     */
    <T> List<T> loadList(Class<T> entityClass, Specification<T> spec);

    /**
     * Load a page of entities matching the given specification with no security checks.
     */
    <T> Page<T> loadPage(Class<T> entityClass, Specification<T> spec, Pageable pageable);

    /**
     * Create a new entity instance with no security checks.
     */
    <T> T create(Class<T> entityClass);

    /**
     * Save an entity with no permission checks.
     */
    <T> T save(T entity);

    /**
     * Delete an entity instance with no permission checks.
     */
    void delete(Object entity);

    /**
     * Delete an entity by class and id with no permission checks.
     */
    void delete(Class<?> entityClass, Object id);
}
