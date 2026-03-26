package com.vn.core.security.data;

import com.vn.core.security.repository.RepositoryRegistry;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
     * Load one entity matching the given specification with no security checks.
     */
    @Override
    @Transactional(readOnly = true)
    public <T> Optional<T> loadOne(Class<T> entityClass, Specification<T> spec) {
        return repositoryRegistry.getSpecificationExecutor(entityClass).findOne(spec);
    }

    /**
     * Load all entities matching the given specification with no security checks.
     */
    @Override
    @Transactional(readOnly = true)
    public <T> List<T> loadList(Class<T> entityClass, Specification<T> spec) {
        return repositoryRegistry.getSpecificationExecutor(entityClass).findAll(spec);
    }

    /**
     * Load a page of entities matching the given specification with no security checks.
     */
    @Override
    @Transactional(readOnly = true)
    public <T> Page<T> loadPage(Class<T> entityClass, Specification<T> spec, Pageable pageable) {
        return repositoryRegistry.getSpecificationExecutor(entityClass).findAll(spec, pageable);
    }

    /**
     * Create a new entity instance with no security checks.
     */
    @Override
    public <T> T create(Class<T> entityClass) {
        try {
            return entityClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create entity: " + entityClass.getName(), e);
        }
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
        delete(entity);
    }

    /**
     * Delete an entity instance with no permission checks.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void delete(Object entity) {
        ((JpaRepository<Object, Object>) repositoryRegistry.getRepository((Class<Object>) entity.getClass())).delete(entity);
    }
}
