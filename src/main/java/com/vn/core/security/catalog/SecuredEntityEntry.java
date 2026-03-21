package com.vn.core.security.catalog;

import com.vn.core.security.permission.EntityOp;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Catalog entry for a single entity type registered for security enforcement.
 */
public record SecuredEntityEntry(
    Class<?> entityClass,
    String code,
    Set<EntityOp> operations,
    List<String> fetchPlanCodes,
    boolean jpqlAllowed
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Class<?> entityClass;
        private String code;
        private Set<EntityOp> operations = EnumSet.noneOf(EntityOp.class);
        private List<String> fetchPlanCodes = List.of();
        private boolean jpqlAllowed = false;

        public Builder entityClass(Class<?> entityClass) {
            this.entityClass = entityClass;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder operations(Set<EntityOp> operations) {
            this.operations = operations;
            return this;
        }

        public Builder fetchPlanCodes(List<String> fetchPlanCodes) {
            this.fetchPlanCodes = fetchPlanCodes;
            return this;
        }

        public Builder jpqlAllowed(boolean jpqlAllowed) {
            this.jpqlAllowed = jpqlAllowed;
            return this;
        }

        public SecuredEntityEntry build() {
            return new SecuredEntityEntry(entityClass, code, operations, fetchPlanCodes, jpqlAllowed);
        }
    }
}
