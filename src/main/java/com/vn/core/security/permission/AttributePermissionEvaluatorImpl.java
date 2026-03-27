package com.vn.core.security.permission;

import com.vn.core.security.MergedSecurityService;
import com.vn.core.security.domain.SecPermission;
import com.vn.core.security.repository.SecPermissionRepository;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link AttributePermissionEvaluator}.
 *
 * <p>Attribute-level permissions default to restrictive: if no permission records exist
 * for the given attribute or its entity wildcard target, access is denied. Matching
 * ALLOW records union across authorities; DENY records do not override another
 * authority's ALLOW.
 */
@Component
public class AttributePermissionEvaluatorImpl implements AttributePermissionEvaluator {

    private static final Logger LOG = LoggerFactory.getLogger(AttributePermissionEvaluatorImpl.class);

    private final SecPermissionRepository secPermissionRepository;
    private final MergedSecurityService mergedSecurityService;

    public AttributePermissionEvaluatorImpl(SecPermissionRepository secPermissionRepository, MergedSecurityService mergedSecurityService) {
        this.secPermissionRepository = secPermissionRepository;
        this.mergedSecurityService = mergedSecurityService;
    }

    @Override
    public boolean canView(Class<?> entityClass, String attribute) {
        return checkAttributePermission(entityClass, attribute, "VIEW");
    }

    @Override
    public boolean canEdit(Class<?> entityClass, String attribute) {
        return checkAttributePermission(entityClass, attribute, "EDIT");
    }

    private boolean checkAttributePermission(Class<?> entityClass, String attribute, String action) {
        Collection<String> authorities = mergedSecurityService.getCurrentUserAuthorityNames();
        if (authorities.isEmpty()) {
            LOG.debug("No authorities for current user - denying attribute {} {} on {}", action, attribute, entityClass.getSimpleName());
            return false;
        }

        String entityTarget = entityClass.getSimpleName().toUpperCase(Locale.ROOT);
        String specificTarget = entityTarget + "." + attribute.toUpperCase(Locale.ROOT);
        String wildcardTarget = entityTarget + ".*";
        List<String> targets = List.of(specificTarget, wildcardTarget);
        List<SecPermission> perms = secPermissionRepository.findByRolesAndTargets(authorities, TargetType.ATTRIBUTE, targets, action);
        if (perms.isEmpty()) {
            LOG.debug("No attribute permission rows found for targets={} action={} - access denied", targets, action);
            return false;
        }

        boolean allowed = perms.stream().anyMatch(p -> "ALLOW".equals(p.getEffect()));
        if (!allowed) {
            LOG.debug("No ALLOW attribute permission found for targets={} action={} - access denied", targets, action);
        }
        return allowed;
    }
}
