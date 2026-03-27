package com.vn.core.security.permission;

/**
 * Defines the kind of target a permission entry applies to.
 * FETCH_PLAN is intentionally excluded — fetch plans are YAML/code-defined only.
 */
public enum TargetType {
    ENTITY,
    ATTRIBUTE,
}
