package com.vn.core.security.merge;

import java.util.Collection;
import java.util.Map;

/**
 * Applies an update payload to an existing entity instance, enforcing
 * attribute-level write permissions before each field is set.
 */
public interface SecureMergeService {
    /**
     * Merge {@code attributes} into {@code entity}, skipping fields the current user
     * is not permitted to edit.
     *
     * @param entity     the existing managed entity instance to update
     * @param attributes map of attribute names to new values
     */
    void mergeForUpdate(Object entity, Map<String, Object> attributes);

    /**
     * Merge the selected attributes from a typed source entity into the existing managed entity.
     *
     * @param entity            the existing managed entity instance to update
     * @param sourceEntity      typed values converted from the request edge
     * @param changedAttributes the top-level attributes that were present in the request body
     */
    void mergeForUpdate(Object entity, Object sourceEntity, Collection<String> changedAttributes);
}
