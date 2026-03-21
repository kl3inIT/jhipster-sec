package com.vn.core.security.repository;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.support.Repositories;
import org.springframework.stereotype.Component;

/**
 * Spring Data {@link Repositories}-backed implementation of {@link RepositoryRegistry}.
 * Dynamically resolves JPA repositories by entity class at runtime.
 */
@Component
public class RepositoriesRegistryImpl implements RepositoryRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(RepositoriesRegistryImpl.class);

    private final ApplicationContext applicationContext;

    private Repositories repositories;

    public RepositoriesRegistryImpl(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    void init() {
        this.repositories = new Repositories(applicationContext);
        LOG.debug("RepositoriesRegistryImpl initialized with Spring Data Repositories");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> JpaRepository<T, ?> getRepository(Class<T> entityClass) {
        return (JpaRepository<T, ?>) repositories
            .getRepositoryFor(entityClass)
            .filter(r -> r instanceof JpaRepository)
            .orElseThrow(() -> new IllegalArgumentException("No JpaRepository registered for entity: " + entityClass.getName()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> JpaSpecificationExecutor<T> getSpecificationExecutor(Class<T> entityClass) {
        return (JpaSpecificationExecutor<T>) repositories
            .getRepositoryFor(entityClass)
            .filter(r -> r instanceof JpaSpecificationExecutor)
            .orElseThrow(() ->
                new IllegalArgumentException("No JpaSpecificationExecutor registered for entity: " + entityClass.getName())
            );
    }
}
