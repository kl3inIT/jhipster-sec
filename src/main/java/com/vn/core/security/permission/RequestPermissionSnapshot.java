package com.vn.core.security.permission;

import com.vn.core.domain.Authority;
import com.vn.core.repository.AuthorityRepository;
import com.vn.core.security.AcceptsGrantedAuthorities;
import com.vn.core.security.domain.SecPermission;
import com.vn.core.security.repository.SecPermissionRepository;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Request-scoped permission snapshot that caches authority validation and permission
 * matrix construction once per HTTP request, eliminating N+1 DB queries on secured
 * entity list operations.
 *
 * <p>This bean is active only within an HTTP request context. Callers outside a request
 * (batch jobs, tests, non-web contexts) must check {@link #isRequestScopeActive()} and
 * fall back to direct repository queries.
 *
 * <p>Per locked decisions D-01 and D-02: the snapshot is request-local only and is
 * destroyed at request end. No cross-request or session-level caching is performed.
 */
@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestPermissionSnapshot {

    private final AuthorityRepository authorityRepository;
    private final SecPermissionRepository secPermissionRepository;

    /** Cached validated authority names for the current request; null if not yet loaded. */
    private Collection<String> cachedAuthorities;

    /** Cached permission matrix for the current request; null if not yet loaded. */
    private PermissionMatrix cachedMatrix;

    public RequestPermissionSnapshot(AuthorityRepository authorityRepository, SecPermissionRepository secPermissionRepository) {
        this.authorityRepository = authorityRepository;
        this.secPermissionRepository = secPermissionRepository;
    }

    /**
     * Returns true when a Spring Web request context is active (i.e., inside an HTTP request).
     * Use this guard before calling snapshot methods from non-web callers.
     */
    public static boolean isRequestScopeActive() {
        return RequestContextHolder.getRequestAttributes() != null;
    }

    /**
     * Returns the current user's validated authority names for this request.
     * Loads from the security context and validates against {@code jhi_authority} on first call;
     * returns the cached result on subsequent calls within the same request.
     */
    public Collection<String> getAuthorities() {
        if (cachedAuthorities == null) {
            cachedAuthorities = loadAuthorities();
        }
        return cachedAuthorities;
    }

    /**
     * Returns the permission matrix for the current request.
     * Builds from a single bulk query on first call; returns the cached result on subsequent calls.
     */
    public PermissionMatrix getMatrix() {
        if (cachedMatrix == null) {
            Collection<String> authorities = getAuthorities();
            if (authorities.isEmpty()) {
                cachedMatrix = PermissionMatrix.EMPTY;
            } else {
                List<SecPermission> allPerms = secPermissionRepository.findAllByAuthorityNameIn(authorities);
                cachedMatrix = new PermissionMatrix(allPerms);
            }
        }
        return cachedMatrix;
    }

    /**
     * Resolves and validates the current user's JWT authority names against the
     * {@code jhi_authority} table, dropping phantom names not backed by a DB row.
     * This mirrors the logic in {@code MergedSecurityContextBridge}.
     */
    private Collection<String> loadAuthorities() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return List.of();
        }
        Set<String> jwtAuthorities = resolveAuthorities(auth).stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        if (jwtAuthorities.isEmpty()) {
            return List.of();
        }
        // Validate against jhi_authority table; drop phantom names not backed by a DB row.
        Set<String> validNames = authorityRepository
            .findAllById(jwtAuthorities)
            .stream()
            .map(Authority::getName)
            .collect(Collectors.toSet());
        return jwtAuthorities.stream().filter(validNames::contains).toList();
    }

    private Collection<? extends GrantedAuthority> resolveAuthorities(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof AcceptsGrantedAuthorities acceptsGrantedAuthorities) {
            return acceptsGrantedAuthorities.getGrantedAuthorities();
        }
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getAuthorities();
        }
        return authentication.getAuthorities();
    }
}
