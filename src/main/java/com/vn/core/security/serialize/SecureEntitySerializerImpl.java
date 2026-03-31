package com.vn.core.security.serialize;

import com.vn.core.security.fetch.FetchPlan;
import com.vn.core.security.fetch.FetchPlanProperty;
import com.vn.core.security.permission.AttributePermissionEvaluator;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;

/**
 * Serializes a JPA entity instance to a map, including only properties
 * declared in the {@link FetchPlan} that the current user is permitted to view.
 * Denied attributes are silently omitted (read-side fail-open per D-15).
 * The {@code id} attribute is always included when present in the fetch plan (D-16).
 *
 * <p>A single {@link BeanWrapperImpl} is instantiated once per entity call and reused
 * for all scalar and association reads, avoiding per-property descriptor overhead.
 * Jackson {@code convertValue} is intentionally not used here because bidirectional
 * JPA associations (e.g. Organization↔Department) cause infinite recursion on full-entity
 * conversion; BeanWrapper reads only the properties requested by the FetchPlan.
 */
@Component
public class SecureEntitySerializerImpl implements SecureEntitySerializer {

    private final AttributePermissionEvaluator attributePermissionEvaluator;

    public SecureEntitySerializerImpl(AttributePermissionEvaluator attributePermissionEvaluator) {
        this.attributePermissionEvaluator = attributePermissionEvaluator;
    }

    @Override
    public Map<String, Object> serialize(Object entity, FetchPlan fetchPlan) {
        if (entity == null) {
            return null;
        }

        Class<?> entityClass = entity.getClass();
        Map<String, Object> values = new LinkedHashMap<>();

        // Instantiate once per entity; reused for both scalar and association reads.
        BeanWrapperImpl wrapper = new BeanWrapperImpl(entity);

        for (FetchPlanProperty property : fetchPlan.getProperties()) {
            String attr = property.name();

            if (property.fetchPlan() != null) {
                // Association/reference property — permission check then recursive serialization.
                if (!attributePermissionEvaluator.canView(entityClass, attr)) {
                    // silently skip denied reference attributes per D-15
                    continue;
                }
                Object refValue = wrapper.getPropertyValue(attr);
                if (refValue == null) {
                    values.put(attr, null);
                } else if (refValue instanceof Collection<?> collection) {
                    values.put(
                        attr,
                        collection.stream().map(item -> serialize(item, property.fetchPlan())).filter(Objects::nonNull).toList()
                    );
                } else {
                    values.put(attr, serialize(refValue, property.fetchPlan()));
                }
            } else {
                // Scalar property — id is always visible per D-16; others require canView.
                if (isAlwaysVisible(attr) || attributePermissionEvaluator.canView(entityClass, attr)) {
                    values.put(attr, wrapper.getPropertyValue(attr));
                }
                // silently skip denied scalar attributes per D-15
            }
        }

        return values;
    }

    private boolean isAlwaysVisible(String attr) {
        return "id".equals(attr.toLowerCase(Locale.ROOT));
    }
}
