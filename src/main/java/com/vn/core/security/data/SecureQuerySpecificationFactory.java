package com.vn.core.security.data;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Builds a limited secured query specification from top-level JSON filters.
 */
@Component
public class SecureQuerySpecificationFactory {

    public <T> Specification<T> build(Class<T> entityClass, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return (root, query, cb) -> null;
        }

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            filters.forEach((fieldName, value) -> predicates.add(cb.equal(root.get(fieldName), value)));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
