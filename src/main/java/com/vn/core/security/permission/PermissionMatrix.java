package com.vn.core.security.permission;

import com.vn.core.security.domain.SecPermission;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable in-memory permission matrix built from a single bulk query.
 * Empty match sets deny access; any matching ALLOW grants access.
 * Attribute checks also honor wildcard ENTITY.* ALLOW rows.
 */
public class PermissionMatrix {

    /** Shared empty instance for unauthenticated or no-permission cases. */
    public static final PermissionMatrix EMPTY = new PermissionMatrix(List.of());

    private final Set<String> allowedKeys;

    public PermissionMatrix(List<SecPermission> permissions) {
        Set<String> allowed = new HashSet<>();
        for (SecPermission p : permissions) {
            if ("ALLOW".equals(p.getEffect())) {
                String key = p.getTargetType().name() + ":" + p.getTarget() + ":" + p.getAction();
                allowed.add(key);
            }
        }
        this.allowedKeys = Set.copyOf(allowed);
    }

    /**
     * Returns true if the current user has ALLOW for the given entity target and action.
     */
    public boolean isEntityPermitted(String target, String action) {
        String key = TargetType.ENTITY.name() + ":" + target + ":" + action;
        return allowedKeys.contains(key);
    }

    /**
     * Returns true if the current user has ALLOW for the given attribute target and action.
     * Also checks wildcard ENTITY.* ALLOW rows.
     */
    public boolean isAttributePermitted(String attrTarget, String action) {
        String key = TargetType.ATTRIBUTE.name() + ":" + attrTarget + ":" + action;
        String entityPart = attrTarget.split("\\.")[0];
        String wildcardKey = TargetType.ATTRIBUTE.name() + ":" + entityPart + ".*:" + action;
        return allowedKeys.contains(key) || allowedKeys.contains(wildcardKey);
    }
}
