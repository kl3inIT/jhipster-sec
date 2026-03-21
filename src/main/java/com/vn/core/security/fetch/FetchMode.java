package com.vn.core.security.fetch;

/**
 * Fetch strategy hint for association loading in a fetch plan.
 */
public enum FetchMode {
    AUTO,
    JOIN,
    BATCH,
    UNDEFINED,
}
