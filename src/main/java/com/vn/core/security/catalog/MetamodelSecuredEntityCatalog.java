package com.vn.core.security.catalog;

import com.vn.core.domain.proof.Department;
import com.vn.core.domain.proof.Employee;
import com.vn.core.domain.proof.Organization;
import com.vn.core.security.permission.EntityOp;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Catalog backed by the live JPA metamodel and gated by {@link SecuredEntity}.
 */
@Component
@Primary
public class MetamodelSecuredEntityCatalog implements SecuredEntityCatalog {

    private static final Map<Class<?>, SecuredEntityEntry> PROOF_ENTRIES = Map.of(
        Organization.class,
        entryFor(Organization.class, "organization", List.of("organization-list", "organization-detail")),
        Department.class,
        entryFor(Department.class, "department", List.of("department-list", "department-detail")),
        Employee.class,
        entryFor(Employee.class, "employee", List.of("employee-list", "employee-detail"))
    );

    private final EntityManager entityManager;

    public MetamodelSecuredEntityCatalog(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<SecuredEntityEntry> entries() {
        return entityManager
            .getMetamodel()
            .getEntities()
            .stream()
            .map(EntityType::getJavaType)
            .filter(this::isSecuredEntity)
            .map(PROOF_ENTRIES::get)
            .filter(Objects::nonNull)
            .toList();
    }

    @Override
    public Optional<SecuredEntityEntry> findByEntityClass(Class<?> entityClass) {
        return entries().stream().filter(entry -> entry.entityClass().equals(entityClass)).findFirst();
    }

    @Override
    public Optional<SecuredEntityEntry> findByCode(String code) {
        return entries().stream().filter(entry -> entry.code().equals(code)).findFirst();
    }

    private boolean isSecuredEntity(Class<?> entityClass) {
        return entityClass.isAnnotationPresent(SecuredEntity.class);
    }

    private static SecuredEntityEntry entryFor(Class<?> entityClass, String code, List<String> fetchPlanCodes) {
        return SecuredEntityEntry
            .builder()
            .entityClass(entityClass)
            .code(code)
            .operations(EnumSet.of(EntityOp.READ, EntityOp.CREATE, EntityOp.UPDATE, EntityOp.DELETE))
            .fetchPlanCodes(fetchPlanCodes)
            .jpqlAllowed(false)
            .build();
    }
}
