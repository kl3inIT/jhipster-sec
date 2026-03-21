package com.vn.core.security.row;

import com.vn.core.security.permission.EntityOp;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Builds a composite JPA {@link Specification} from all applicable row-level policies
 * for a given entity class and operation.
 *
 * <p>All policies are AND-composed so every policy constraint must be satisfied.
 * Per D-10, row policies are applied as database-level constraints.
 */
@Component
public class RowLevelSpecificationBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(RowLevelSpecificationBuilder.class);

    private final RowLevelPolicyProvider rowLevelPolicyProvider;

    public RowLevelSpecificationBuilder(RowLevelPolicyProvider rowLevelPolicyProvider) {
        this.rowLevelPolicyProvider = rowLevelPolicyProvider;
    }

    /**
     * Builds an AND-composed {@link Specification} from all row policies applicable
     * to the given entity class and operation.
     *
     * @param entityClass the entity class
     * @param op          the CRUD operation
     * @return a composed specification, or {@code Specification.where(null)} if no policies apply
     */
    public <T> Specification<T> build(Class<T> entityClass, EntityOp op) {
        List<RowPolicyDefinition> policies = rowLevelPolicyProvider.getPolicies(entityClass.getSimpleName(), op);
        if (policies.isEmpty()) {
            LOG.debug("No row policies for entity={} op={}", entityClass.getSimpleName(), op);
            return Specification.where(null);
        }
        LOG.debug("Composing {} row policy/policies for entity={} op={}", policies.size(), entityClass.getSimpleName(), op);
        Specification<T> combined = Specification.where(null);
        for (RowPolicyDefinition def : policies) {
            combined = combined.and(def.getSpecification());
        }
        return combined;
    }
}
