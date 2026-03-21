package com.vn.core.security.permission;

import com.vn.core.security.MergedSecurityService;
import com.vn.core.security.repository.SecPermissionRepository;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Database-backed implementation of {@link RolePermissionService}.
 * Applies DENY-wins semantics per D-07: if any permission record has effect DENY,
 * the operation is denied regardless of ALLOW records.
 */
@Service
public class RolePermissionServiceDbImpl implements RolePermissionService {

    private static final Logger LOG = LoggerFactory.getLogger(RolePermissionServiceDbImpl.class);

    private final SecPermissionRepository secPermissionRepository;
    private final MergedSecurityService mergedSecurityService;

    public RolePermissionServiceDbImpl(
        SecPermissionRepository secPermissionRepository,
        MergedSecurityService mergedSecurityService
    ) {
        this.secPermissionRepository = secPermissionRepository;
        this.mergedSecurityService = mergedSecurityService;
    }

    @Override
    public boolean isEntityOpPermitted(Class<?> entityClass, EntityOp op) {
        Collection<String> authorities = mergedSecurityService.getCurrentUserAuthorityNames();
        if (authorities.isEmpty()) {
            LOG.debug("No authorities for current user — denying entity op {} on {}", op, entityClass.getSimpleName());
            return false;
        }
        String target = entityClass.getSimpleName().toUpperCase(Locale.ROOT);
        return hasPermission(authorities, TargetType.ENTITY, target, op.name());
    }

    @Override
    public boolean hasPermission(Collection<String> authorityNames, TargetType targetType, String target, String action) {
        List<com.vn.core.security.domain.SecPermission> perms = secPermissionRepository.findByRolesAndTarget(
            authorityNames,
            targetType,
            target,
            action
        );
        // DENY-wins: any DENY record blocks access regardless of ALLOW records
        boolean hasDeny = perms.stream().anyMatch(p -> "DENY".equals(p.getEffect()));
        if (hasDeny) {
            LOG.debug("DENY permission found for target={} action={} — access denied", target, action);
            return false;
        }
        return perms.stream().anyMatch(p -> "ALLOW".equals(p.getEffect()));
    }
}
