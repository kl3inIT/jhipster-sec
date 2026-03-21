package com.vn.core.security.merge;

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
}
