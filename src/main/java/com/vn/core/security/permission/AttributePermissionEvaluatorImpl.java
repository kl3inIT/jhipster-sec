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
 * for the given attribute, access is denied. The permission matrix stores only GRANT records,
 * so an empty result means no grant was given for this attribute.
 * When records exist, DENY-wins semantics apply.
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
            LOG.debug("No authorities for current user — denying attribute {} {} on {}", action, attribute, entityClass.getSimpleName());
            return false;
        }
        String target =
            entityClass.getSimpleName().toUpperCase(Locale.ROOT) + "." + attribute.toUpperCase(Locale.ROOT);
        List<SecPermission> perms = secPermissionRepository.findByRolesAndTarget(authorities, TargetType.ATTRIBUTE, target, action);
        // No GRANT record = denied. The permission matrix stores only GRANT records,
        // so empty results means no grant was given for this attribute.
        if (perms.isEmpty()) {
            return false;
        }
        // DENY-wins when rules exist
        boolean hasDeny = perms.stream().anyMatch(p -> "DENY".equals(p.getEffect()));
        if (hasDeny) {
            LOG.debug("DENY attribute permission found for target={} action={}", target, action);
            return false;
        }
        return perms.stream().anyMatch(p -> "ALLOW".equals(p.getEffect()));
    }
}
