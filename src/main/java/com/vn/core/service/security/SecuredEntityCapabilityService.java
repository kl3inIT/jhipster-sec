package com.vn.core.service.security;

import com.vn.core.security.MergedSecurityService;
import com.vn.core.security.catalog.SecuredEntityCatalog;
import com.vn.core.security.catalog.SecuredEntityEntry;
import com.vn.core.security.domain.SecPermission;
import com.vn.core.security.permission.EntityOp;
import com.vn.core.security.permission.PermissionMatrix;
import com.vn.core.security.repository.SecPermissionRepository;
import com.vn.core.service.dto.security.SecuredAttributeCapabilityDTO;
import com.vn.core.service.dto.security.SecuredEntityCapabilityDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Aggregates current-user entity and attribute capabilities for secured entities.
 * Capability answers follow default-deny plus union-of-ALLOW semantics.
 */
@Service
public class SecuredEntityCapabilityService {

    private final SecuredEntityCatalog securedEntityCatalog;
    private final EntityManager entityManager;
    private final MergedSecurityService mergedSecurityService;
    private final SecPermissionRepository secPermissionRepository;

    public SecuredEntityCapabilityService(
        SecuredEntityCatalog securedEntityCatalog,
        EntityManager entityManager,
        MergedSecurityService mergedSecurityService,
        SecPermissionRepository secPermissionRepository
    ) {
        this.securedEntityCatalog = securedEntityCatalog;
        this.entityManager = entityManager;
        this.mergedSecurityService = mergedSecurityService;
        this.secPermissionRepository = secPermissionRepository;
    }

    public List<SecuredEntityCapabilityDTO> getCurrentUserCapabilities() {
        Collection<String> authorities = mergedSecurityService.getCurrentUserAuthorityNames();
        PermissionMatrix matrix;
        if (authorities.isEmpty()) {
            matrix = PermissionMatrix.EMPTY;
        } else {
            List<SecPermission> allPerms = secPermissionRepository.findAllByAuthorityNameIn(authorities);
            matrix = new PermissionMatrix(allPerms);
        }
        PermissionMatrix finalMatrix = matrix;
        return securedEntityCatalog
            .entries()
            .stream()
            .sorted(Comparator.comparing(SecuredEntityEntry::code))
            .map(entry -> toDto(entry, finalMatrix))
            .toList();
    }

    private SecuredEntityCapabilityDTO toDto(SecuredEntityEntry entry, PermissionMatrix matrix) {
        String target = entry.entityClass().getSimpleName().toUpperCase(Locale.ROOT);
        SecuredEntityCapabilityDTO dto = new SecuredEntityCapabilityDTO();
        dto.setCode(entry.code());
        dto.setCanCreate(matrix.isEntityPermitted(target, EntityOp.CREATE.name()));
        dto.setCanRead(matrix.isEntityPermitted(target, EntityOp.READ.name()));
        dto.setCanUpdate(matrix.isEntityPermitted(target, EntityOp.UPDATE.name()));
        dto.setCanDelete(matrix.isEntityPermitted(target, EntityOp.DELETE.name()));
        dto.setAttributes(attributesFor(entry, matrix, target));
        return dto;
    }

    private List<SecuredAttributeCapabilityDTO> attributesFor(SecuredEntityEntry entry, PermissionMatrix matrix, String entityTarget) {
        return entityManager
            .getMetamodel()
            .entity(entry.entityClass())
            .getAttributes()
            .stream()
            .map(Attribute::getName)
            .sorted()
            .map(attribute -> {
                String attrTarget = entityTarget + "." + attribute.toUpperCase(Locale.ROOT);
                SecuredAttributeCapabilityDTO dto = new SecuredAttributeCapabilityDTO();
                dto.setName(attribute);
                dto.setCanView(matrix.isAttributePermitted(attrTarget, "VIEW"));
                dto.setCanEdit(matrix.isAttributePermitted(attrTarget, "EDIT"));
                return dto;
            })
            .toList();
    }

}
