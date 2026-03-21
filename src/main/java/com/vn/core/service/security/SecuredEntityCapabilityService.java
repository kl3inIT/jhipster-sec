package com.vn.core.service.security;

import com.vn.core.security.catalog.SecuredEntityCatalog;
import com.vn.core.security.catalog.SecuredEntityEntry;
import com.vn.core.security.permission.AttributePermissionEvaluator;
import com.vn.core.security.permission.EntityOp;
import com.vn.core.security.permission.EntityPermissionEvaluator;
import com.vn.core.service.dto.security.SecuredAttributeCapabilityDTO;
import com.vn.core.service.dto.security.SecuredEntityCapabilityDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Aggregates current-user entity and attribute capabilities for secured entities.
 */
@Service
public class SecuredEntityCapabilityService {

    private final SecuredEntityCatalog securedEntityCatalog;
    private final EntityPermissionEvaluator entityPermissionEvaluator;
    private final AttributePermissionEvaluator attributePermissionEvaluator;
    private final EntityManager entityManager;

    public SecuredEntityCapabilityService(
        SecuredEntityCatalog securedEntityCatalog,
        EntityPermissionEvaluator entityPermissionEvaluator,
        AttributePermissionEvaluator attributePermissionEvaluator,
        EntityManager entityManager
    ) {
        this.securedEntityCatalog = securedEntityCatalog;
        this.entityPermissionEvaluator = entityPermissionEvaluator;
        this.attributePermissionEvaluator = attributePermissionEvaluator;
        this.entityManager = entityManager;
    }

    public List<SecuredEntityCapabilityDTO> getCurrentUserCapabilities() {
        return securedEntityCatalog.entries().stream().sorted(Comparator.comparing(SecuredEntityEntry::code)).map(this::toDto).toList();
    }

    private SecuredEntityCapabilityDTO toDto(SecuredEntityEntry entry) {
        SecuredEntityCapabilityDTO dto = new SecuredEntityCapabilityDTO();
        dto.setCode(entry.code());
        dto.setCanCreate(entityPermissionEvaluator.isPermitted(entry.entityClass(), EntityOp.CREATE));
        dto.setCanRead(entityPermissionEvaluator.isPermitted(entry.entityClass(), EntityOp.READ));
        dto.setCanUpdate(entityPermissionEvaluator.isPermitted(entry.entityClass(), EntityOp.UPDATE));
        dto.setCanDelete(entityPermissionEvaluator.isPermitted(entry.entityClass(), EntityOp.DELETE));
        dto.setAttributes(attributesFor(entry));
        return dto;
    }

    private List<SecuredAttributeCapabilityDTO> attributesFor(SecuredEntityEntry entry) {
        return entityManager
            .getMetamodel()
            .entity(entry.entityClass())
            .getAttributes()
            .stream()
            .map(Attribute::getName)
            .sorted()
            .map(attribute -> attributeToDto(entry.entityClass(), attribute))
            .toList();
    }

    private SecuredAttributeCapabilityDTO attributeToDto(Class<?> entityClass, String attribute) {
        SecuredAttributeCapabilityDTO dto = new SecuredAttributeCapabilityDTO();
        dto.setName(attribute);
        dto.setCanView(attributePermissionEvaluator.canView(entityClass, attribute));
        dto.setCanEdit(attributePermissionEvaluator.canEdit(entityClass, attribute));
        return dto;
    }
}
