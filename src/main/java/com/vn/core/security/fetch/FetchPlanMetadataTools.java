package com.vn.core.security.fetch;

import jakarta.persistence.Entity;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for fetch-plan metadata operations.
 * Provides helpers to inspect entity properties and identify JPA entity types.
 */
public final class FetchPlanMetadataTools {

    private FetchPlanMetadataTools() {
        // utility class
    }

    /**
     * Returns the JavaBean property names for the given class, excluding {@code class}.
     *
     * @param entityClass the class to inspect
     * @return list of property names
     */
    public static List<String> getPropertyNames(Class<?> entityClass) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(entityClass);
            return Arrays.stream(beanInfo.getPropertyDescriptors())
                .map(PropertyDescriptor::getName)
                .filter(name -> !"class".equals(name))
                .toList();
        } catch (IntrospectionException e) {
            throw new IllegalArgumentException("Cannot introspect entity class: " + entityClass.getName(), e);
        }
    }

    /**
     * Returns {@code true} if the given type is annotated with {@code @Entity}.
     *
     * @param type the class to test
     * @return true if it is a JPA entity
     */
    public static boolean isEntityType(Class<?> type) {
        return type.isAnnotationPresent(Entity.class);
    }
}
