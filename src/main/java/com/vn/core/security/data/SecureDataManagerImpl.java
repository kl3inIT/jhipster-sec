package com.vn.core.security.data;

import com.vn.core.security.catalog.SecuredEntityCatalog;
import com.vn.core.security.catalog.SecuredEntityEntry;
import com.vn.core.security.fetch.FetchPlan;
import com.vn.core.security.fetch.FetchPlanResolver;
import com.vn.core.security.merge.SecureMergeService;
import com.vn.core.security.permission.EntityOp;
import com.vn.core.security.row.RowLevelSpecificationBuilder;
import com.vn.core.security.serialize.SecureEntitySerializer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Central enforcement orchestrator for security-protected data access.
 *
 * <p>All CRUD operations on protected entities flow through this class which
 * composes row-level policies, fetch-plan resolution, attribute serialization,
 * and secure merge into coherent read/write/delete pipelines.
 */
@Service
@Transactional
public class SecureDataManagerImpl implements SecureDataManager {

    private final DataManager dataManager;
    private final SecuredEntityCatalog catalog;
    private final RowLevelSpecificationBuilder rowLevelSpecificationBuilder;
    private final FetchPlanResolver fetchPlanResolver;
    private final SecureEntitySerializer secureEntitySerializer;
    private final SecureMergeService secureMergeService;

    public SecureDataManagerImpl(
        DataManager dataManager,
        SecuredEntityCatalog catalog,
        RowLevelSpecificationBuilder rowLevelSpecificationBuilder,
        FetchPlanResolver fetchPlanResolver,
        SecureEntitySerializer secureEntitySerializer,
        SecureMergeService secureMergeService
    ) {
        this.dataManager = dataManager;
        this.catalog = catalog;
        this.rowLevelSpecificationBuilder = rowLevelSpecificationBuilder;
        this.fetchPlanResolver = fetchPlanResolver;
        this.secureEntitySerializer = secureEntitySerializer;
        this.secureMergeService = secureMergeService;
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Page<Map<String, Object>> loadList(String entityCode, String fetchPlanCode, Pageable pageable) {
        SecuredEntityEntry entry = resolveEntry(entityCode);
        Class<Object> entityClass = (Class<Object>) entry.entityClass();
        Specification<Object> rowSpec = rowLevelSpecificationBuilder.build(entityClass, EntityOp.READ);
        Page<Object> page = dataManager.loadPage(entityClass, rowSpec, pageable, EntityOp.READ);

        FetchPlan fetchPlan = fetchPlanResolver.resolve(entityClass, fetchPlanCode);
        List<Map<String, Object>> serializedList = new ArrayList<>();
        for (Object entity : page.getContent()) {
            serializedList.add(secureEntitySerializer.serialize(entity, fetchPlan));
        }
        return new PageImpl<>(serializedList, pageable, page.getTotalElements());
    }

    /**
     * READ flow: row-policy spec -> DataManager-secured query execution -> fetch-plan resolve -> serialize.
     */
    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public <T> Page<Map<String, Object>> loadByQuery(SecuredLoadQuery query) {
        SecuredEntityEntry entry = resolveEntry(query.entityCode());
        Class<Object> entityClass = (Class<Object>) entry.entityClass();

        Specification<Object> rowSpec = rowLevelSpecificationBuilder.build(entityClass, EntityOp.READ);
        Specification<Object> finalSpec = rowSpec;
        if (query.jpql() != null && !query.jpql().isBlank()) {
            if (!entry.jpqlAllowed()) {
                throw new AccessDeniedException("JPQL queries not allowed for " + query.entityCode());
            }
            throw new AccessDeniedException("JPQL query translation is not implemented for secured queries: " + query.jpql());
        }

        Page<Object> page = dataManager.loadPage(entityClass, finalSpec, query.pageable(), EntityOp.READ);
        FetchPlan fetchPlan = fetchPlanResolver.resolve(entityClass, query.fetchPlanCode());

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
        Class<Object> entityClass = (Class<Object>) entry.entityClass();

        Specification<Object> idSpec = (root, query, cb) -> cb.equal(root.get("id"), id);
        Specification<Object> rowSpec = rowLevelSpecificationBuilder.build(entityClass, EntityOp.READ);

        return dataManager
            .loadOne(entityClass, idSpec.and(rowSpec), EntityOp.READ)
            .map(entity -> {
                FetchPlan fetchPlan = fetchPlanResolver.resolve(entityClass, fetchPlanCode);
                return secureEntitySerializer.serialize(entity, fetchPlan);
            });
    }

    /**
     * WRITE flow: CRUD check or row-constrained target lookup -> merge -> unconstrained save -> serialize.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, Object> save(String entityCode, Object id, Map<String, Object> attributes, String fetchPlanCode) {
        SecuredEntityEntry entry = resolveEntry(entityCode);
        Class<Object> entityClass = (Class<Object>) entry.entityClass();
        UnconstrainedDataManager unconstrainedDataManager;

        Object entity;
        if (id == null) {
            dataManager.checkCrud(entityClass, EntityOp.CREATE);
            unconstrainedDataManager = dataManager.unconstrained();
            entity = unconstrainedDataManager.create(entityClass);
        } else {
            Specification<Object> idSpec = (root, query, cb) -> cb.equal(root.get("id"), id);
            Specification<Object> rowSpec = rowLevelSpecificationBuilder.build(entityClass, EntityOp.UPDATE);
            entity = dataManager
                .loadOne(entityClass, idSpec.and(rowSpec), EntityOp.UPDATE)
                .orElseThrow(() -> new AccessDeniedException("UPDATE denied - entity not found or row policy restricts access"));
            unconstrainedDataManager = dataManager.unconstrained();
        }

        secureMergeService.mergeForUpdate(entity, attributes);
        Object saved = unconstrainedDataManager.save(entity);

        FetchPlan fetchPlan = fetchPlanResolver.resolve(entityClass, fetchPlanCode);
        return secureEntitySerializer.serialize(saved, fetchPlan);
    }

    /**
     * DELETE flow: row-constrained lookup -> explicit unconstrained delete.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void delete(String entityCode, Object id) {
        SecuredEntityEntry entry = resolveEntry(entityCode);
        Class<Object> entityClass = (Class<Object>) entry.entityClass();

        Specification<Object> idSpec = (root, query, cb) -> cb.equal(root.get("id"), id);
        Specification<Object> rowSpec = rowLevelSpecificationBuilder.build(entityClass, EntityOp.DELETE);
        Object entity = dataManager
            .loadOne(entityClass, idSpec.and(rowSpec), EntityOp.DELETE)
            .orElseThrow(() -> new AccessDeniedException("DELETE denied - entity not found or row policy restricts access"));

        dataManager.unconstrained().delete(entity);
    }

    private SecuredEntityEntry resolveEntry(String entityCode) {
        return catalog
            .findByCode(entityCode)
            .orElseThrow(() -> new IllegalArgumentException("Entity code not in secured catalog: " + entityCode));
    }
}
