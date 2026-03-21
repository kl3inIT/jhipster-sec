package com.vn.core.config;

import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;

public interface DatabaseTestcontainer {
    @Container
    PostgreSQLContainer databaseContainer = new PostgreSQLContainer("postgres:18.3")
        .withDatabaseName("jhipster-sec")
        .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(DatabaseTestcontainer.class)))
        .withReuse(true);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", databaseContainer::getJdbcUrl);
        registry.add("spring.datasource.username", databaseContainer::getUsername);
        registry.add("spring.datasource.password", databaseContainer::getPassword);
    }
}
