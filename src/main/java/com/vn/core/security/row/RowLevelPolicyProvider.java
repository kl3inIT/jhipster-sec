package com.vn.core.security.row;

import com.vn.core.security.permission.EntityOp;
import java.util.List;

/**
 * Provides row-level policy definitions applicable to a given entity and operation.
 */
public interface RowLevelPolicyProvider {
    /**
     * Return all active row policies for the given entity name and CRUD operation.
     *
     * @param entityName logical entity name (e.g. "Organization")
     * @param operation  the CRUD operation being performed
     * @return list of applicable policy definitions; empty if none apply
     */
    List<RowPolicyDefinition> getPolicies(String entityName, EntityOp operation);
}
