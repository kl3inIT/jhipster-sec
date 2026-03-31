package com.vn.core.service.security;

import com.vn.core.security.catalog.SecuredEntityCatalog;
import com.vn.core.security.catalog.SecuredEntityEntry;
import com.vn.core.security.permission.TargetType;
import com.vn.core.service.dto.security.SecPermissionDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Translates between the permission matrix UI contract and runtime-compatible stored permissions.
 */
@Service
public class SecPermissionUiContractService {

    private static final String GRANT = "GRANT";
    private static final String ALLOW = "ALLOW";
    private static final String ATTRIBUTE_WILDCARD = ".*";

    private final SecuredEntityCatalog securedEntityCatalog;
    private final EntityManager entityManager;

    public SecPermissionUiContractService(SecuredEntityCatalog securedEntityCatalog, EntityManager entityManager) {
        this.securedEntityCatalog = securedEntityCatalog;
        this.entityManager = entityManager;
    }

    public SecPermissionDTO normalizeIncoming(SecPermissionDTO dto) {
        SecPermissionDTO normalized = copy(dto);
        normalized.setTarget(normalizeTargetToStored(dto.getTargetType(), dto.getTarget()));
        normalized.setEffect(normalizeIncomingEffect(dto.getEffect()));
        return normalized;
    }

    public SecPermissionDTO normalizeOutgoing(SecPermissionDTO dto) {
        SecPermissionDTO normalized = copy(dto);
        normalized.setTarget(normalizeTargetToUi(dto.getTargetType(), dto.getTarget()));
        normalized.setEffect(normalizeOutgoingEffect(dto.getEffect()));
        return normalized;
    }

    private String normalizeTargetToStored(String targetType, String target) {
        if (!isSupportedTargetType(targetType) || target == null) {
            return target;
        }
        return uiToStoredTargets().getOrDefault(target.toLowerCase(Locale.ROOT), target);
    }

    private String normalizeTargetToUi(String targetType, String target) {
        if (!isSupportedTargetType(targetType) || target == null) {
            return target;
        }
        return storedToUiTargets().getOrDefault(target.toUpperCase(Locale.ROOT), target);
    }

    private String normalizeIncomingEffect(String effect) {
        if (GRANT.equals(effect)) {
            return ALLOW;
        }
        return effect;
    }

    private String normalizeOutgoingEffect(String effect) {
        if (ALLOW.equals(effect)) {
            return GRANT;
        }
        return effect;
    }

    private boolean isSupportedTargetType(String targetType) {
        return TargetType.ENTITY.name().equals(targetType) || TargetType.ATTRIBUTE.name().equals(targetType);
    }

    private Map<String, String> uiToStoredTargets() {
        Map<String, String> mappings = new HashMap<>();
        for (SecuredEntityEntry entry : securedEntityCatalog.entries()) {
            String uiEntityTarget = entry.code();
            String storedEntityTarget = entry.entityClass().getSimpleName().toUpperCase(Locale.ROOT);
            mappings.put(uiEntityTarget.toLowerCase(Locale.ROOT), storedEntityTarget);
            mappings.put((uiEntityTarget + ATTRIBUTE_WILDCARD).toLowerCase(Locale.ROOT), storedEntityTarget + ATTRIBUTE_WILDCARD);
            entityManager
                .getMetamodel()
                .entity(entry.entityClass())
                .getAttributes()
                .stream()
                .map(Attribute::getName)
                .forEach(attribute ->
                    mappings.put(
                        (uiEntityTarget + "." + attribute).toLowerCase(Locale.ROOT),
                        storedEntityTarget + "." + attribute.toUpperCase(Locale.ROOT)
                    )
                );
        }
        return mappings;
    }

    private Map<String, String> storedToUiTargets() {
        // Build directly from catalog so attribute names preserve their original camelCase form.
        // Inverting uiToStoredTargets() would return the lowercase key used as input for that map,
        // causing camelCase attributes (e.g. "DEPARTMENT.COSTCENTER" -> "department.costcenter"
        // instead of the correct "department.costCenter") to silently miss frontend Map lookups.
        Map<String, String> mappings = new HashMap<>();
        for (SecuredEntityEntry entry : securedEntityCatalog.entries()) {
            String uiEntityTarget = entry.code();
            String storedEntityTarget = entry.entityClass().getSimpleName().toUpperCase(Locale.ROOT);
            mappings.put(storedEntityTarget, uiEntityTarget);
            mappings.put(storedEntityTarget + ATTRIBUTE_WILDCARD, uiEntityTarget + ATTRIBUTE_WILDCARD);
            entityManager
                .getMetamodel()
                .entity(entry.entityClass())
                .getAttributes()
                .stream()
                .map(Attribute::getName)
                .forEach(attribute ->
                    mappings.put(storedEntityTarget + "." + attribute.toUpperCase(Locale.ROOT), uiEntityTarget + "." + attribute)
                );
        }
        return mappings;
    }

    private SecPermissionDTO copy(SecPermissionDTO dto) {
        SecPermissionDTO copy = new SecPermissionDTO();
        copy.setId(dto.getId());
        copy.setAuthorityName(dto.getAuthorityName());
        copy.setTargetType(dto.getTargetType());
        copy.setTarget(dto.getTarget());
        copy.setAction(dto.getAction());
        copy.setEffect(dto.getEffect());
        return copy;
    }
}
