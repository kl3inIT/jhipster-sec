package com.vn.core.security.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Default (empty) implementation of {@link SecuredEntityCatalog}.
 * Returns no registered entities — Phase 4 will provide a concrete {@code @Primary}
 * catalog with sample entity registrations.
 */
@Component
public class DefaultSecuredEntityCatalog implements SecuredEntityCatalog {

    @Override
    public List<SecuredEntityEntry> entries() {
        return List.of();
    }

    @Override
    public Optional<SecuredEntityEntry> findByEntityClass(Class<?> entityClass) {
        return entries().stream().filter(e -> e.entityClass().equals(entityClass)).findFirst();
    }

    @Override
    public Optional<SecuredEntityEntry> findByCode(String code) {
        return entries().stream().filter(e -> e.code().equals(code)).findFirst();
    }
}
