package com.vn.core.security.catalog;

import java.util.List;
import java.util.Optional;

/**
 * Allowlist catalog of entity types that participate in security enforcement.
 * Only entities registered here are subject to CRUD, attribute, and row-level checks.
 */
public interface SecuredEntityCatalog {
    /**
     * @return all registered entity entries.
     */
    List<SecuredEntityEntry> entries();

    /**
     * Look up an entry by its entity class.
     */
    Optional<SecuredEntityEntry> findByEntityClass(Class<?> entityClass);

    /**
     * Look up an entry by its logical code.
     */
    Optional<SecuredEntityEntry> findByCode(String code);
}
