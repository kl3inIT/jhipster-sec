package com.vn.core.security.data;

import com.vn.core.security.catalog.SecuredEntityCatalog;
import com.vn.core.security.catalog.SecuredEntityEntry;
import com.vn.core.security.fetch.FetchPlan;
import com.vn.core.security.fetch.FetchPlanResolver;
import com.vn.core.security.merge.SecureMergeService;
import com.vn.core.security.permission.EntityOp;
import com.vn.core.security.serialize.SecureEntitySerializer;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;

/**
 * Central enforcement orchestrator for security-protected data access.
 *
 * <p>All CRUD operations on protected entities flow through this class which
 * composes row-level policies, typed merge, and explicit edge serialization
 * into coherent read/write/delete pipelines.
 */
@Service
@Transactional
public class SecureDataManagerImpl implements SecureDataManager {

    private final DataManager dataManager;
    private final SecuredEntityCatalog catalog;
    private final FetchPlanResolver fetchPlanResolver;
    private final SecureEntitySerializer secureEntitySerializer;
    private final SecureMergeService secureMergeService;
    private final SecureQuerySpecificationFactory secureQuerySpecificationFactory;

    public SecureDataManagerImpl(
        DataManager dataManager,
        SecuredEntityCatalog catalog,
        FetchPlanResolver fetchPlanResolver,
        SecureEntitySerializer secureEntitySerializer,
        SecureMergeService secureMergeService,
        SecureQuerySpecificationFactory secureQuerySpecificationFactory
    ) {
        this.dataManager = dataManager;
        this.catalog = catalog;
        this.fetchPlanResolver = fetchPlanResolver;
        this.secureEntitySerializer = secureEntitySerializer;
        this.secureMergeService = secureMergeService;
        this.secureQuerySpecificationFactory = secureQuerySpecificationFactory;
    }

    @Override
    @Transactional(readOnly = true)
    public <E> Page<E> loadList(Class<E> entityClass, Pageable pageable) {
        resolveEntry(entityClass);
        return loadListInternal(entityClass, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public <E> Page<E> loadByQuery(Class<E> entityClass, SecuredLoadQuery query) {
        SecuredEntityEntry entry = resolveEntry(entityClass);
        if (query.entityCode() != null && !entry.code().equals(query.entityCode())) {
            throw new IllegalArgumentException("Query entity code does not match typed entity: " + query.entityCode());
        }
        return loadByQueryInternal(entityClass, query, entry);
    }

    @Override
    @Transactional(readOnly = true)
    public <E> Optional<E> loadOne(Class<E> entityClass, Object id) {
        resolveEntry(entityClass);
        return loadOneInternal(entityClass, id);
    }

    @Override
    public <E> E save(Class<E> entityClass, Object id, EntityMutation<E> mutation) {
        resolveEntry(entityClass);
        return saveInternal(entityClass, id, mutation);
    }

    @Override
    public void delete(Class<?> entityClass, Object id) {
        resolveEntry(entityClass);
        deleteInternal(entityClass, id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Map<String, Object>> loadList(String entityCode, String fetchPlanCode, Pageable pageable) {
        Class<Object> entityClass = resolveEntityClass(entityCode);
        return loadListInternal(entityClass, pageable).map(entity -> serialize(entity, fetchPlanCode));
    }

    @Override
    @Transactional(readOnly = true)
    public <T> Page<Map<String, Object>> loadByQuery(SecuredLoadQuery query) {
        SecuredEntityEntry entry = resolveEntry(query.entityCode());
        Class<Object> entityClass = resolveEntityClass(query.entityCode());
        return loadByQueryInternal(entityClass, query, entry).map(entity -> serialize(entity, query.fetchPlanCode()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> loadOne(String entityCode, Object id, String fetchPlanCode) {
        Class<Object> entityClass = resolveEntityClass(entityCode);
        return loadOneInternal(entityClass, id).map(entity -> serialize(entity, fetchPlanCode));
    }

    @Override
    public void delete(String entityCode, Object id) {
        deleteInternal(resolveEntityClass(entityCode), id);
    }

    private <E> Page<E> loadListInternal(Class<E> entityClass, Pageable pageable) {
        return dataManager.loadPage(entityClass, null, pageable, EntityOp.READ);
    }

    private <E> Page<E> loadByQueryInternal(Class<E> entityClass, SecuredLoadQuery query, SecuredEntityEntry entry) {
        if (query.jpql() != null && !query.jpql().isBlank()) {
            if (!entry.jpqlAllowed()) {
                throw new AccessDeniedException("JPQL queries not allowed for " + query.entityCode());
            }
            throw new AccessDeniedException("JPQL query translation is not implemented for secured queries: " + query.jpql());
        }

        Specification<E> filterSpec = secureQuerySpecificationFactory.build(entityClass, query.parameters());
        return dataManager.loadPage(entityClass, filterSpec, query.pageable(), EntityOp.READ);
    }

    private <E> Optional<E> loadOneInternal(Class<E> entityClass, Object id) {
        Specification<E> idSpec = (root, query, cb) -> cb.equal(root.get("id"), id);
        return dataManager.loadOne(entityClass, idSpec, EntityOp.READ);
    }

    private <E> E saveInternal(Class<E> entityClass, Object id, EntityMutation<E> mutation) {
        if (mutation == null || mutation.entity() == null) {
            throw new IllegalArgumentException("Typed entity mutation is required");
        }

        E entity;
        if (id == null) {
            dataManager.checkCrud(entityClass, EntityOp.CREATE);
            entity = dataManager.unconstrained().create(entityClass);
        } else {
            Specification<E> idSpec = (root, query, cb) -> cb.equal(root.get("id"), id);
            entity = dataManager
                .loadOne(entityClass, idSpec, EntityOp.UPDATE)
                .orElseThrow(() -> new AccessDeniedException("UPDATE denied - entity not found or not accessible"));
        }

        secureMergeService.mergeForUpdate(entity, mutation.entity(), mutation.changedAttributes());
        return dataManager.unconstrained().save(entity);
    }

    private SecuredEntityEntry resolveEntry(String entityCode) {
        return catalog
            .findByCode(entityCode)
            .orElseThrow(() -> new IllegalArgumentException("Entity code not in secured catalog: " + entityCode));
    }

    private SecuredEntityEntry resolveEntry(Class<?> entityClass) {
        return catalog
            .findByEntityClass(entityClass)
            .orElseThrow(() -> new IllegalArgumentException("Entity type not in secured catalog: " + entityClass.getName()));
    }

    @SuppressWarnings("unchecked")
    private <E> Class<E> resolveEntityClass(String entityCode) {
        return (Class<E>) resolveEntry(entityCode).entityClass();
    }

    private Map<String, Object> serialize(Object entity, String fetchPlanCode) {
        FetchPlan fetchPlan = fetchPlanResolver.resolve(ClassUtils.getUserClass(entity), fetchPlanCode);
        return secureEntitySerializer.serialize(entity, fetchPlan);
    }

    private <E> void deleteInternal(Class<E> entityClass, Object id) {
        Specification<E> idSpec = (root, query, cb) -> cb.equal(root.get("id"), id);
        E entity = dataManager
            .loadOne(entityClass, idSpec, EntityOp.DELETE)
            .orElseThrow(() -> new AccessDeniedException("DELETE denied - entity not found or not accessible"));

        dataManager.unconstrained().delete(entity);
    }
}
