package com.vn.core.service.security;

import com.vn.core.security.MergedSecurityService;
import com.vn.core.security.catalog.SecuredEntityCatalog;
import com.vn.core.security.catalog.SecuredEntityEntry;
import com.vn.core.security.domain.SecPermission;
import com.vn.core.security.permission.EntityOp;
import com.vn.core.security.permission.TargetType;
import com.vn.core.security.repository.SecPermissionRepository;
import com.vn.core.service.dto.security.SecuredAttributeCapabilityDTO;
import com.vn.core.service.dto.security.SecuredEntityCapabilityDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Aggregates current-user entity and attribute capabilities for secured entities.
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

    /**
     * In-memory permission matrix built from a single bulk query.
     * Preserves existing permission semantics:
     * <ul>
     *   <li>Entity-level: DENY-wins, no ALLOW record = denied</li>
     *   <li>Attribute-level: deny-default (no records = denied), DENY-wins when records exist,
     *       wildcard ENTITY.* pattern supported for ALLOW</li>
     * </ul>
     */
    private static class PermissionMatrix {

        static final PermissionMatrix EMPTY = new PermissionMatrix(List.of());

        private final Set<String> allowedKeys;
        private final Set<String> deniedKeys;

        PermissionMatrix(List<SecPermission> permissions) {
            Set<String> allowed = new HashSet<>();
            Set<String> denied = new HashSet<>();
            for (SecPermission p : permissions) {
                String key = p.getTargetType().name() + ":" + p.getTarget() + ":" + p.getAction();
                if ("DENY".equals(p.getEffect())) {
                    denied.add(key);
                } else if ("ALLOW".equals(p.getEffect())) {
                    allowed.add(key);
                }
            }
            this.allowedKeys = Set.copyOf(allowed);
            this.deniedKeys = Set.copyOf(denied);
        }

        /**
         * Entity-level: DENY-wins, no ALLOW = denied.
         */
        boolean isEntityPermitted(String target, String action) {
            String key = TargetType.ENTITY.name() + ":" + target + ":" + action;
            if (deniedKeys.contains(key)) {
                return false;
            }
            return allowedKeys.contains(key);
        }

        /**
         * Attribute-level: deny-default (no records = denied), DENY-wins when records exist.
         * Supports wildcard ENTITY.* pattern for ALLOW.
         */
        boolean isAttributePermitted(String attrTarget, String action) {
            String key = TargetType.ATTRIBUTE.name() + ":" + attrTarget + ":" + action;
            String entityPart = attrTarget.split("\\.")[0];
            String wildcardKey = TargetType.ATTRIBUTE.name() + ":" + entityPart + ".*:" + action;

            // DENY-wins for specific or wildcard
            if (deniedKeys.contains(key) || deniedKeys.contains(wildcardKey)) {
                return false;
            }
            // ALLOW for specific or wildcard
            if (allowedKeys.contains(key) || allowedKeys.contains(wildcardKey)) {
                return true;
            }
            // No matching records = denied (deny-default)
            return false;
        }
    }
}
