package com.vn.core.security.row;

import org.springframework.data.jpa.domain.Specification;

/**
 * Contract for a row-level policy definition that contributes a JPA {@link Specification}
 * to the security pipeline for a specific entity and operation.
 */
public interface RowPolicyDefinition {
    /**
     * @return the JPA Specification that implements this policy's row filter
     */
    <T> Specification<T> getSpecification();
}
