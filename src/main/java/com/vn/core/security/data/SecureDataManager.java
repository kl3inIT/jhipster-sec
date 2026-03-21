package com.vn.core.security.data;

import java.util.List;
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
     * Load a page of entities applying CRUD, row-level, and attribute checks.
     * Prefer this over {@link #loadByQuery} for standard paginated list operations.
     *
     * @param entityCode    logical code of the entity type
     * @param fetchPlanCode fetch plan to use when serializing the response
     * @param pageable      pagination parameters
     * @return page of serialized entities visible to the caller
     */
    Page<Map<String, Object>> loadList(String entityCode, String fetchPlanCode, Pageable pageable);

    /**
     * Load a page of entities matching the query, serialized per the requested fetch plan.
     * Use when you need query-building features such as additional filter conditions.
     * For plain paginated lists prefer {@link #loadList}.
     */
    <T> Page<Map<String, Object>> loadByQuery(SecuredLoadQuery query);

    /**
     * Load a single entity by id, applying CRUD, row-level, and attribute checks.
     *
     * @param entityCode    logical code of the entity type
     * @param id            entity id to load
     * @param fetchPlanCode fetch plan to use when serializing the response
     * @return serialized entity when visible to the caller; empty when the row is missing or filtered by row policy
     */
    Optional<Map<String, Object>> loadOne(String entityCode, Object id, String fetchPlanCode);

    /**
     * Save (create or update) an entity from a payload map, enforcing attribute-level write guards.
     *
     * @param entityCode    logical code of the entity type
     * @param id            entity id for updates; null for new records
     * @param attributes    field values to apply
     * @param fetchPlanCode fetch plan to use when serializing the saved entity for the response
     * @return serialized entity after save
     */
    <T> Map<String, Object> save(String entityCode, Object id, Map<String, Object> attributes, String fetchPlanCode);

    /**
     * Delete an entity by code and id, after confirming delete permission.
     */
    void delete(String entityCode, Object id);
}
