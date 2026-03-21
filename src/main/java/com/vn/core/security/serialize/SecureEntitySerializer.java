package com.vn.core.security.serialize;

import com.vn.core.security.fetch.FetchPlan;
import java.util.Map;

/**
 * Serializes a JPA entity instance to a map, including only properties
 * declared in the {@link FetchPlan} that the current user is permitted to view.
 */
public interface SecureEntitySerializer {
    /**
     * Serialize {@code entity} according to {@code fetchPlan}, applying attribute
     * visibility checks for the current security context.
     *
     * @param entity    the entity instance to serialize
     * @param fetchPlan the fetch plan defining which properties to include
     * @return a map of permitted attribute names to their values
     */
    Map<String, Object> serialize(Object entity, FetchPlan fetchPlan);
}
