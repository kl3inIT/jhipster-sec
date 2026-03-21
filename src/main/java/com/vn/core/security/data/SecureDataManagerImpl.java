package com.vn.core.security.data;

import com.vn.core.security.access.AccessManager;
import com.vn.core.security.access.CrudEntityContext;
import com.vn.core.security.catalog.SecuredEntityCatalog;
import com.vn.core.security.catalog.SecuredEntityEntry;
import com.vn.core.security.fetch.FetchPlan;
import com.vn.core.security.fetch.FetchPlanResolver;
import com.vn.core.security.merge.SecureMergeService;
import com.vn.core.security.permission.EntityOp;
import com.vn.core.security.repository.RepositoryRegistry;
import com.vn.core.security.row.RowLevelSpecificationBuilder;
import com.vn.core.security.serialize.SecureEntitySerializer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Central enforcement orchestrator for security-protected data access.
 *
 * <p>All CRUD operations on protected entities flow through this class which
 * composes CRUD checks, row-level policies, fetch-plan resolution, attribute
 * serialization, and secure merge into coherent read/write/delete pipelines
 * per D-04/D-05/D-06.
 */
@Service
@Transactional
public class SecureDataManagerImpl implements SecureDataManager {

    private final AccessManager accessManager;
    private final SecuredEntityCatalog catalog;
    private final RowLevelSpecificationBuilder rowLevelSpecificationBuilder;
    private final FetchPlanResolver fetchPlanResolver;
    private final SecureEntitySerializer secureEntitySerializer;
    private final SecureMergeService secureMergeService;
    private final RepositoryRegistry repositoryRegistry;

    public SecureDataManagerImpl(
        AccessManager accessManager,
        SecuredEntityCatalog catalog,
        RowLevelSpecificationBuilder rowLevelSpecificationBuilder,
        FetchPlanResolver fetchPlanResolver,
        SecureEntitySerializer secureEntitySerializer,
        SecureMergeService secureMergeService,
        RepositoryRegistry repositoryRegistry
    ) {
        this.accessManager = accessManager;
        this.catalog = catalog;
        this.rowLevelSpecificationBuilder = rowLevelSpecificationBuilder;
        this.fetchPlanResolver = fetchPlanResolver;
        this.secureEntitySerializer = secureEntitySerializer;
        this.secureMergeService = secureMergeService;
        this.repositoryRegistry = repositoryRegistry;
    }

    /**
     * READ flow per D-05: CRUD check -> row-policy spec -> query execution -> fetch-plan resolve -> serialize.
     */
    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public <T> Page<Map<String, Object>> loadByQuery(SecuredLoadQuery query) {
        // Step 0 — Resolve catalog entry
        SecuredEntityEntry entry = resolveEntry(query.entityCode());
        Class<?> entityClass = entry.entityClass();

        // Step 1 — CRUD check
        checkCrud(entityClass, EntityOp.READ);

        // Step 2 — Row-policy specification
        Specification<Object> rowSpec = (Specification<Object>) rowLevelSpecificationBuilder.build(entityClass, EntityOp.READ);

        // Step 3 — Query execution
        JpaSpecificationExecutor<Object> specRepo = (JpaSpecificationExecutor<Object>) repositoryRegistry.getSpecificationExecutor(
            entityClass
        );

        Specification<Object> finalSpec = rowSpec;
        if (query.jpql() != null && !query.jpql().isBlank()) {
            if (!entry.jpqlAllowed()) {
                throw new AccessDeniedException("JPQL queries not allowed for " + query.entityCode());
            }
            throw new AccessDeniedException("JPQL query translation is not implemented for secured queries: " + query.jpql());
        }

        Page<Object> page = specRepo.findAll(finalSpec, query.pageable());

        // Step 4 — Fetch plan resolution
        FetchPlan fetchPlan = fetchPlanResolver.resolve(entityClass, query.fetchPlanCode());

        // Step 5 — Attribute-filtered serialization
        List<Map<String, Object>> serializedList = new ArrayList<>();
        for (Object entity : page.getContent()) {
            serializedList.add(secureEntitySerializer.serialize(entity, fetchPlan));
        }
        return new PageImpl<>(serializedList, query.pageable(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> loadOne(String entityCode, Object id, String fetchPlanCode) {
        SecuredEntityEntry entry = resolveEntry(entityCode);
        Class<?> entityClass = entry.entityClass();

        checkCrud(entityClass, EntityOp.READ);

        JpaSpecificationExecutor<Object> specRepo = (JpaSpecificationExecutor<Object>) repositoryRegistry.getSpecificationExecutor(
            entityClass
        );
        Specification<Object> idSpec = (root, query, cb) -> cb.equal(root.get("id"), id);
        Specification<Object> rowSpec = (Specification<Object>) rowLevelSpecificationBuilder.build(entityClass, EntityOp.READ);

        return specRepo
            .findOne(idSpec.and(rowSpec))
            .map(entity -> {
                FetchPlan fetchPlan = fetchPlanResolver.resolve(entityClass, fetchPlanCode);
                return secureEntitySerializer.serialize(entity, fetchPlan);
            });
    }

    /**
     * WRITE flow per D-06: CRUD check -> row-constrained target lookup -> merge -> save -> serialize.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, Object> save(String entityCode, Object id, Map<String, Object> attributes, String fetchPlanCode) {
        // Step 0 — Resolve catalog entry
        SecuredEntityEntry entry = resolveEntry(entityCode);
        Class<?> entityClass = entry.entityClass();

        // Determine operation
        EntityOp op = (id == null) ? EntityOp.CREATE : EntityOp.UPDATE;

        // Step 1 — CRUD check
        checkCrud(entityClass, op);

        Object entity;
        if (op == EntityOp.UPDATE) {
            // Step 2 — Row-constrained target lookup for UPDATE
            JpaSpecificationExecutor<Object> specRepo = (JpaSpecificationExecutor<Object>) repositoryRegistry.getSpecificationExecutor(
                entityClass
            );
            Specification<Object> idSpec = (root, query, cb) -> cb.equal(root.get("id"), id);
            Specification<Object> rowSpec = (Specification<Object>) rowLevelSpecificationBuilder.build(entityClass, EntityOp.UPDATE);
            entity = specRepo
                .findOne(idSpec.and(rowSpec))
                .orElseThrow(() -> new AccessDeniedException("UPDATE denied — entity not found or row policy restricts access"));
        } else {
            // Step 2 — CREATE: instantiate new entity
            try {
                entity = entityClass.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Could not instantiate entity " + entityClass.getSimpleName(), e);
            }
        }

        // Step 3 — Attribute edit enforcement (merge)
        secureMergeService.mergeForUpdate(entity, attributes);

        // Step 4 — Persistence
        JpaRepository<Object, Object> repo = (JpaRepository<Object, Object>) repositoryRegistry.getRepository(entityClass);
        Object saved = repo.save(entity);

        // Step 5 — Secure re-read
        FetchPlan fetchPlan = fetchPlanResolver.resolve(entityClass, fetchPlanCode);
        return secureEntitySerializer.serialize(saved, fetchPlan);
    }

    /**
     * DELETE flow: CRUD check -> row-constrained lookup -> delete.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void delete(String entityCode, Object id) {
        // Step 0 — Resolve catalog entry
        SecuredEntityEntry entry = resolveEntry(entityCode);
        Class<?> entityClass = entry.entityClass();

        // Step 1 — CRUD check
        checkCrud(entityClass, EntityOp.DELETE);

        // Step 2 — Row-constrained target lookup
        JpaSpecificationExecutor<Object> specRepo = (JpaSpecificationExecutor<Object>) repositoryRegistry.getSpecificationExecutor(
            entityClass
        );
        Specification<Object> idSpec = (root, query, cb) -> cb.equal(root.get("id"), id);
        Specification<Object> rowSpec = (Specification<Object>) rowLevelSpecificationBuilder.build(entityClass, EntityOp.DELETE);
        Object entity = specRepo
            .findOne(idSpec.and(rowSpec))
            .orElseThrow(() -> new AccessDeniedException("DELETE denied — entity not found or row policy restricts access"));

        // Step 3 — Delete
        JpaRepository<Object, Object> repo = (JpaRepository<Object, Object>) repositoryRegistry.getRepository(entityClass);
        repo.delete(entity);
    }

    private SecuredEntityEntry resolveEntry(String entityCode) {
        return catalog
            .findByCode(entityCode)
            .orElseThrow(() -> new IllegalArgumentException("Entity code not in secured catalog: " + entityCode));
    }

    private void checkCrud(Class<?> entityClass, EntityOp op) {
        CrudEntityContext ctx = new CrudEntityContext(entityClass, op);
        accessManager.applyRegisteredConstraints(ctx);
        if (!ctx.isPermitted()) {
            throw new AccessDeniedException(op.name() + " denied on " + entityClass.getSimpleName());
        }
    }
}
