package com.vn.core;

import com.vn.core.config.AsyncSyncConfiguration;
import com.vn.core.config.DatabaseTestcontainer;
import com.vn.core.config.JacksonConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    classes = {
        JhipsterSecApp.class,
        JacksonConfiguration.class,
        AsyncSyncConfiguration.class,
        com.vn.core.config.JacksonHibernateConfiguration.class,
    }
)
@ImportTestcontainers(DatabaseTestcontainer.class)
public @interface IntegrationTest {}
