package com.vn.core.security.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Central registry to obtain Spring Data repositories by entity class.
 * Used by the security data pipeline to load and persist entities
 * without direct coupling to individual repository beans.
 */
public interface RepositoryRegistry {
    /**
     * @return the {@link JpaRepository} for the given entity class
     * @throws IllegalArgumentException if no repository is registered for the class
     */
    <T> JpaRepository<T, ?> getRepository(Class<T> entityClass);

    /**
     * @return the {@link JpaSpecificationExecutor} for the given entity class
     * @throws IllegalArgumentException if no specification executor is registered for the class
     */
    <T> JpaSpecificationExecutor<T> getSpecificationExecutor(Class<T> entityClass);
}
