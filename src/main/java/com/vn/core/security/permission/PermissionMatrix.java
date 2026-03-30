package com.vn.core.security.permission;

import com.vn.core.security.domain.SecPermission;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable in-memory permission matrix built from a single bulk query.
 *
 * <p>Default-deny + union-of-ALLOW semantics: access is granted only when at least one matching
 * ALLOW key is present. Supports two Jmix-style cascades:
 * <ul>
 *   <li><b>Entity wildcard</b>: {@code ENTITY:*:action} grants the action on every entity.</li>
 *   <li><b>Edit-implies-view</b>: {@code ATTRIBUTE:target:EDIT} (or its wildcard) implicitly
 *       grants VIEW on the same attribute.</li>
 * </ul>
 */
public class PermissionMatrix {

    /** Shared empty instance for unauthenticated or no-permission cases. */
    public static final PermissionMatrix EMPTY = new PermissionMatrix(List.of());

    private static final String ENTITY = TargetType.ENTITY.name();
    private static final String ATTRIBUTE = TargetType.ATTRIBUTE.name();

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
     * Also checks entity-level wildcard {@code *} that grants the action on all entities.
     */
    public boolean isEntityPermitted(String target, String action) {
        String key = ENTITY + ":" + target + ":" + action;
        String wildcardKey = ENTITY + ":*:" + action;
        return allowedKeys.contains(key) || allowedKeys.contains(wildcardKey);
    }

    /**
     * Returns true if the current user has ALLOW for the given attribute target and action.
     *
     * <p>Checks (in order):
     * <ol>
     *   <li>Direct match on the specific attribute + action.</li>
     *   <li>Attribute wildcard {@code ENTITY.*} for the same action.</li>
     *   <li>Edit-implies-view: if {@code action} is {@code VIEW}, also checks EDIT on the same
     *       targets (direct and wildcard), because modify permission subsumes read permission.</li>
     * </ol>
     */
    public boolean isAttributePermitted(String attrTarget, String action) {
        String key = ATTRIBUTE + ":" + attrTarget + ":" + action;
        String entityPart = attrTarget.split("\\.")[0];
        String wildcardKey = ATTRIBUTE + ":" + entityPart + ".*:" + action;
        if (allowedKeys.contains(key) || allowedKeys.contains(wildcardKey)) {
            return true;
        }
        // Edit-implies-view: VIEW is implicitly granted when EDIT is granted.
        if ("VIEW".equals(action)) {
            String editKey = ATTRIBUTE + ":" + attrTarget + ":EDIT";
            String editWildcardKey = ATTRIBUTE + ":" + entityPart + ".*:EDIT";
            return allowedKeys.contains(editKey) || allowedKeys.contains(editWildcardKey);
        }
        return false;
    }
}
