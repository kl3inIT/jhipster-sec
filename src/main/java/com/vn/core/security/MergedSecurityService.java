package com.vn.core.security;

import java.util.Collection;
import java.util.Optional;

/**
 * Security service contract for the merged security engine.
 * Phase 3 enforcement beans program against this interface.
 * Ported from angapp SecurityService.
 */
public interface MergedSecurityService {
    /** Returns validated authority names for the current user (phantom roles filtered). */
    Collection<String> getCurrentUserAuthorityNames();

    /** Returns the login of the current user, or empty if unauthenticated. */
    Optional<String> getCurrentUserLogin();

    /** Returns true if the current context is authenticated. */
    boolean isAuthenticated();
}
