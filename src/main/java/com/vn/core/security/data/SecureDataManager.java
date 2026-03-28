package com.vn.core.security.data;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Central entry point for security-enforced data access.
 * All CRUD operations on protected entities must go through this interface.
 */
public interface SecureDataManager {
    /**
     * Typed write payload that preserves both the converted entity values and the
     * top-level attributes present in the incoming request body.
     */
    record EntityMutation<E>(E entity, Collection<String> changedAttributes) {}

    /**
     * Load a page of real entity instances after CRUD and row-level checks.
     */
    <E> Page<E> loadList(Class<E> entityClass, Pageable pageable);

    /**
     * Load a page of real entity instances matching the secured query.
     */
    <E> Page<E> loadByQuery(Class<E> entityClass, SecuredLoadQuery query);

    /**
     * Load one real entity instance by id after CRUD and row-level checks.
     */
    <E> Optional<E> loadOne(Class<E> entityClass, Object id);

    /**
     * Save a typed entity mutation after CRUD, row-level, and attribute checks.
     */
    <E> E save(Class<E> entityClass, Object id, EntityMutation<E> mutation);

    /**
     * Delete an entity by type and id after confirming delete permission.
     */
    void delete(Class<?> entityClass, Object id);

    /**
     * Load a page of entities applying CRUD, row-level, and attribute checks.
     * Prefer this over {@link #loadByQuery} for standard paginated list operations.
     *
     * @param entityCode    logical code of the entity type
     * @param fetchPlanCode fetch plan to use when serializing the response
     * @param pageable      pagination parameters
     * @return page of serialized entities visible to the caller
     */
    @Deprecated(since = "08.3-03")
    Page<Map<String, Object>> loadList(String entityCode, String fetchPlanCode, Pageable pageable);

    /**
     * Load a page of entities matching the query, serialized per the requested fetch plan.
     * Use when you need query-building features such as additional filter conditions.
     * For plain paginated lists prefer {@link #loadList}.
     */
    @Deprecated(since = "08.3-03")
    <T> Page<Map<String, Object>> loadByQuery(SecuredLoadQuery query);

    /**
     * Load a single entity by id, applying CRUD, row-level, and attribute checks.
     *
     * @param entityCode    logical code of the entity type
     * @param id            entity id to load
     * @param fetchPlanCode fetch plan to use when serializing the response
     * @return serialized entity when visible to the caller; empty when the row is missing or inaccessible
     */
    @Deprecated(since = "08.3-03")
    Optional<Map<String, Object>> loadOne(String entityCode, Object id, String fetchPlanCode);

    /**
     * Delete an entity by code and id, after confirming delete permission.
     */
    @Deprecated(since = "08.3-03")
    void delete(String entityCode, Object id);
}
