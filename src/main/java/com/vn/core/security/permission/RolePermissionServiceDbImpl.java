package com.vn.core.security.permission;

import com.vn.core.security.MergedSecurityService;
import com.vn.core.security.domain.SecPermission;
import com.vn.core.security.repository.SecPermissionRepository;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Database-backed implementation of {@link RolePermissionService}.
 * Applies default-deny plus union-of-ALLOW semantics: access is granted only when
 * at least one matching permission record has effect ALLOW.
 */
@Service
public class RolePermissionServiceDbImpl implements RolePermissionService {

    private static final Logger LOG = LoggerFactory.getLogger(RolePermissionServiceDbImpl.class);

    private final SecPermissionRepository secPermissionRepository;
    private final MergedSecurityService mergedSecurityService;
    private final RequestPermissionSnapshot requestPermissionSnapshot;

    public RolePermissionServiceDbImpl(
        SecPermissionRepository secPermissionRepository,
        MergedSecurityService mergedSecurityService,
        RequestPermissionSnapshot requestPermissionSnapshot
    ) {
        this.secPermissionRepository = secPermissionRepository;
        this.mergedSecurityService = mergedSecurityService;
        this.requestPermissionSnapshot = requestPermissionSnapshot;
    }

    @Override
    public boolean isEntityOpPermitted(Class<?> entityClass, EntityOp op) {
        String target = entityClass.getSimpleName().toUpperCase(Locale.ROOT);
        // Use request-scoped snapshot when available: single matrix lookup replaces per-entity DB query.
        if (RequestPermissionSnapshot.isRequestScopeActive()) {
            PermissionMatrix matrix = requestPermissionSnapshot.getMatrix();
            boolean permitted = matrix.isEntityPermitted(target, op.name());
            if (!permitted) {
                LOG.debug("Snapshot: no ALLOW for entity op {} on {} - access denied", op, entityClass.getSimpleName());
            }
            return permitted;
        }
        // Fallback for non-web contexts.
        Collection<String> authorities = mergedSecurityService.getCurrentUserAuthorityNames();
        if (authorities.isEmpty()) {
            LOG.debug("No authorities for current user - denying entity op {} on {}", op, entityClass.getSimpleName());
            return false;
        }
        return hasPermission(authorities, TargetType.ENTITY, target, op.name());
    }

    @Override
    public boolean hasPermission(Collection<String> authorityNames, TargetType targetType, String target, String action) {
        List<SecPermission> perms = secPermissionRepository.findByRolesAndTarget(authorityNames, targetType, target, action);
        if (perms.isEmpty()) {
            LOG.debug("No permission rows found for target={} action={} - access denied", target, action);
            return false;
        }
        boolean allowed = perms.stream().anyMatch(p -> "ALLOW".equals(p.getEffect()));
        if (!allowed) {
            LOG.debug("No ALLOW permission found for target={} action={} - access denied", target, action);
        }
        return allowed;
    }
}
