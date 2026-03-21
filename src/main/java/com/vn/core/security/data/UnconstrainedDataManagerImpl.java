package com.vn.core.security.data;

import com.vn.core.security.repository.RepositoryRegistry;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trusted data access implementation that bypasses all security enforcement.
 *
 * <p>This is the explicit trusted bypass path per D-02. It exposes raw repository
 * access with no CRUD checks, no row-level policies, no attribute filtering, and
 * no fetch-plan shaping. Use only from internal infrastructure code that has
 * already enforced its own access controls (e.g., row-policy evaluation, system tasks).
 */
@Service
@Transactional
public class UnconstrainedDataManagerImpl implements UnconstrainedDataManager {

    private final RepositoryRegistry repositoryRegistry;

    public UnconstrainedDataManagerImpl(RepositoryRegistry repositoryRegistry) {
        this.repositoryRegistry = repositoryRegistry;
    }

    /**
     * Load an entity by class and id with no security checks.
     *
     * @throws EntityNotFoundException if no entity with the given id exists
     */
    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public <T> T load(Class<T> entityClass, Object id) {
        return ((JpaRepository<T, Object>) repositoryRegistry.getRepository(entityClass)).findById(id).orElseThrow(() ->
            new EntityNotFoundException(entityClass.getSimpleName() + " not found: " + id)
        );
    }

    /**
     * Load all entities of the given class with no security checks.
     */
    @Override
    @Transactional(readOnly = true)
    public <T> List<T> loadAll(Class<T> entityClass) {
        return repositoryRegistry.getRepository(entityClass).findAll();
    }

    /**
     * Save an entity with no permission checks.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T save(T entity) {
        return ((JpaRepository<T, Object>) repositoryRegistry.getRepository(entity.getClass())).save(entity);
    }

    /**
     * Delete an entity by class and id with no permission checks.
     *
     * @throws EntityNotFoundException if no entity with the given id exists
     */
    @Override
    @SuppressWarnings("unchecked")
    public void delete(Class<?> entityClass, Object id) {
        Object entity = load((Class<Object>) entityClass, id);
        ((JpaRepository<Object, Object>) repositoryRegistry.getRepository(entityClass)).delete(entity);
    }
}
