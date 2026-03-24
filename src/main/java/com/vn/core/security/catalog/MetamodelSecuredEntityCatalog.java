package com.vn.core.security.catalog;

import com.vn.core.security.permission.EntityOp;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Catalog backed by the live JPA metamodel and gated by {@link SecuredEntity}.
 */
@Component
@Primary
public class MetamodelSecuredEntityCatalog implements SecuredEntityCatalog {

    private final List<SecuredEntityEntry> cachedEntries;

    public MetamodelSecuredEntityCatalog(EntityManager entityManager) {
        this.cachedEntries = Collections.unmodifiableList(
            entityManager
                .getMetamodel()
                .getEntities()
                .stream()
                .map(EntityType::getJavaType)
                .filter(cls -> cls.isAnnotationPresent(SecuredEntity.class))
                .map(MetamodelSecuredEntityCatalog::buildEntry)
                .sorted(Comparator.comparing(SecuredEntityEntry::code))
                .toList()
        );
    }

    @Override
    public List<SecuredEntityEntry> entries() {
        return cachedEntries;
    }

    @Override
    public Optional<SecuredEntityEntry> findByEntityClass(Class<?> entityClass) {
        return cachedEntries.stream().filter(entry -> entry.entityClass().equals(entityClass)).findFirst();
    }

    @Override
    public Optional<SecuredEntityEntry> findByCode(String code) {
        return cachedEntries.stream().filter(entry -> entry.code().equals(code)).findFirst();
    }

    private static SecuredEntityEntry buildEntry(Class<?> entityClass) {
        SecuredEntity annotation = entityClass.getAnnotation(SecuredEntity.class);
        String code = annotation.code().isEmpty()
            ? entityClass.getSimpleName().toLowerCase(Locale.ROOT)
            : annotation.code();
        List<String> fetchPlanCodes = annotation.fetchPlanCodes().length > 0
            ? List.of(annotation.fetchPlanCodes())
            : List.of(code + "-list", code + "-detail");
        return SecuredEntityEntry.builder()
            .entityClass(entityClass)
            .code(code)
            .operations(EnumSet.of(EntityOp.READ, EntityOp.CREATE, EntityOp.UPDATE, EntityOp.DELETE))
            .fetchPlanCodes(fetchPlanCodes)
            .jpqlAllowed(false)
            .build();
    }
}
