package com.vn.core.security.bridge;

import java.util.Collection;
import java.util.Optional;

/**
 * Bridge between the JHipster SecurityContext and the merged security engine.
 * <p>
 * Phase 1 provides a default implementation backed by {@link com.vn.core.security.SecurityUtils}.
 * Phase 2 overrides this by providing its own Spring bean implementing this interface.
 */
public interface SecurityContextBridge {
    /**
     * Returns the login name of the currently authenticated user, or empty if not authenticated.
     */
    Optional<String> getCurrentUserLogin();

    /**
     * Returns the raw authority names (e.g. "ROLE_ADMIN") for the current user,
     * or an empty collection if not authenticated.
     */
    Collection<String> getCurrentUserAuthorities();

    /**
     * Returns true if the current security context holds an authenticated (non-anonymous) principal.
     */
    boolean isAuthenticated();
}
