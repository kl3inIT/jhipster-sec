package com.vn.core.security.access;

import com.vn.core.security.permission.EntityOp;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

/**
 * Access context for row-level constraints expressed as JPA Criteria {@link Predicate}s.
 */
public class RowLevelAccessContext implements AccessContext {

    private final Class<?> entityClass;
    private final EntityOp operation;
    private final List<Predicate> predicates;

    public RowLevelAccessContext(Class<?> entityClass, EntityOp operation) {
        this.entityClass = entityClass;
        this.operation = operation;
        this.predicates = new ArrayList<>();
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public EntityOp getOperation() {
        return operation;
    }

    public List<Predicate> getPredicates() {
        return predicates;
    }

    public void addPredicate(Predicate predicate) {
        this.predicates.add(predicate);
    }
}
