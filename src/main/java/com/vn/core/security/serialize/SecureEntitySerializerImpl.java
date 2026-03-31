package com.vn.core.security.serialize;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
 * <p>D-09: Scalar property values are read from a single Jackson {@link ObjectNode} projection
 * that is produced once per entity, replacing the per-property {@code BeanWrapperImpl.getPropertyValue}
 * call for flat scalars. Association traversal still uses {@link BeanWrapperImpl} to preserve
 * the typed Java object reference needed for recursive {@link #serialize} calls.
 */
@Component
public class SecureEntitySerializerImpl implements SecureEntitySerializer {

    private final AttributePermissionEvaluator attributePermissionEvaluator;
    private final ObjectMapper objectMapper;

    public SecureEntitySerializerImpl(AttributePermissionEvaluator attributePermissionEvaluator, ObjectMapper objectMapper) {
        this.attributePermissionEvaluator = attributePermissionEvaluator;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> serialize(Object entity, FetchPlan fetchPlan) {
        if (entity == null) {
            return null;
        }

        Class<?> entityClass = entity.getClass();
        Map<String, Object> values = new LinkedHashMap<>();

        // D-09: convert the entity to a Jackson ObjectNode once for scalar field reads,
        // avoiding per-property BeanWrapper descriptor reflection on the hot scalar path.
        ObjectNode entityNode = objectMapper.convertValue(entity, ObjectNode.class);

        // BeanWrapper is kept for association traversal only, to preserve typed object
        // references needed for recursive serialize calls.
        BeanWrapperImpl wrapper = null;

        for (FetchPlanProperty property : fetchPlan.getProperties()) {
            String attr = property.name();

            if (property.fetchPlan() != null) {
                // Association/reference property — permission check then recursive serialization.
                // Uses BeanWrapper to preserve typed Java references for recursive calls.
                if (!attributePermissionEvaluator.canView(entityClass, attr)) {
                    // silently skip denied reference attributes per D-15
                    continue;
                }
                if (wrapper == null) {
                    wrapper = new BeanWrapperImpl(entity);
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
                // D-09: read scalars from the pre-built ObjectNode rather than BeanWrapper.
                if (isAlwaysVisible(attr) || attributePermissionEvaluator.canView(entityClass, attr)) {
                    values.put(attr, readScalar(entityNode, attr));
                }
                // silently skip denied scalar attributes per D-15
            }
        }

        return values;
    }

    /**
     * Reads a scalar value from the pre-built Jackson {@link ObjectNode}.
     * Returns {@code null} if the field is absent or explicitly null in the JSON representation.
     */
    private Object readScalar(ObjectNode entityNode, String fieldName) {
        var fieldNode = entityNode.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }
        if (fieldNode.isNumber()) {
            return fieldNode.numberValue();
        }
        if (fieldNode.isBoolean()) {
            return fieldNode.booleanValue();
        }
        if (fieldNode.isTextual()) {
            return fieldNode.textValue();
        }
        // For any other type (object, array) fall back to standard value
        return objectMapper.convertValue(fieldNode, Object.class);
    }

    private boolean isAlwaysVisible(String attr) {
        return "id".equals(attr.toLowerCase(Locale.ROOT));
    }
}
