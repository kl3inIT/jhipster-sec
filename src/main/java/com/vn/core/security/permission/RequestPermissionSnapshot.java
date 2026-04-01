package com.vn.core.security.permission;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.vn.core.security.AcceptsGrantedAuthorities;
import com.vn.core.security.domain.SecPermission;
import com.vn.core.security.repository.SecPermissionRepository;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
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
 * matrix construction once per HTTP request, and shares a cross-request PermissionMatrix
 * cache in Hazelcast keyed by the user's JWT authority-name set.
 *
 * <p>This bean is active only within an HTTP request context. Callers outside a request
 * (batch jobs, tests, non-web contexts) must check {@link #isRequestScopeActive()} and
 * fall back to direct repository queries.
 *
 * <p>Per locked decisions D-01 through D-06:
 * <ul>
 *   <li>JWT authority names are trusted directly with no {@code jhi_authority} DB lookup (D-05, D-06).
 *       An authority deleted from {@code jhi_authority} takes effect when the user's JWT expires —
 *       which is the accepted revocation bound for this application.</li>
 *   <li>The {@link PermissionMatrix} is shared across requests via Hazelcast, keyed by the sorted
 *       authority-name set (D-01, D-04).</li>
 *   <li>The shared cache must be evicted on every {@code SecPermission} create, update, or delete
 *       so permission changes take effect within the next HTTP request (D-02, D-03). The Hazelcast
 *       map TTL is a non-semantic safety ceiling only — correctness comes from write-path eviction.</li>
 * </ul>
 */
@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestPermissionSnapshot {

    /** Name of the Hazelcast map used as the cross-request PermissionMatrix cache. */
    public static final String PERMISSION_MATRIX_CACHE = "sec-permission-matrix";

    private final SecPermissionRepository secPermissionRepository;
    private final HazelcastInstance hazelcastInstance;

    /** Cached authority names for the current request; null if not yet loaded. */
    private Collection<String> cachedAuthorities;

    /** Cached permission matrix for the current request; null if not yet loaded. */
    private PermissionMatrix cachedMatrix;

    public RequestPermissionSnapshot(SecPermissionRepository secPermissionRepository, HazelcastInstance hazelcastInstance) {
        this.secPermissionRepository = secPermissionRepository;
        this.hazelcastInstance = hazelcastInstance;
    }

    /**
     * Returns true when a Spring Web request context is active (i.e., inside an HTTP request).
     * Use this guard before calling snapshot methods from non-web callers.
     */
    public static boolean isRequestScopeActive() {
        return RequestContextHolder.getRequestAttributes() != null;
    }

    /**
     * Returns the current user's authority names for this request, taken directly from the JWT.
     * No {@code jhi_authority} DB validation is performed per D-05 and D-06.
     * Returns the cached result on subsequent calls within the same request.
     */
    public Collection<String> getAuthorities() {
        if (cachedAuthorities == null) {
            cachedAuthorities = loadAuthorities();
        }
        return cachedAuthorities;
    }

    /**
     * Returns the permission matrix for the current request.
     *
     * <p>On first call, derives a deterministic cache key from the sorted JWT authority names,
     * checks the shared Hazelcast cache, and only queries the DB if no cached entry exists.
     * Returns the same instance on subsequent calls within the same request.
     */
    public PermissionMatrix getMatrix() {
        if (cachedMatrix == null) {
            Collection<String> authorities = getAuthorities();
            if (authorities.isEmpty()) {
                cachedMatrix = PermissionMatrix.EMPTY;
            } else {
                String cacheKey = toCacheKey(authorities);
                IMap<String, PermissionMatrix> cache = hazelcastInstance.getMap(PERMISSION_MATRIX_CACHE);
                cachedMatrix = cache.computeIfAbsent(cacheKey, k -> buildMatrix(authorities));
            }
        }
        return cachedMatrix;
    }

    /**
     * Resolves the current user's JWT authority names directly from the security context.
     * Per D-05 and D-06, no DB validation is performed; the token is already signature-verified
     * by Spring Security's filter chain before this method is called.
     */
    private Collection<String> loadAuthorities() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return List.of();
        }
        Set<String> jwtAuthorities = resolveAuthorities(auth).stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        return List.copyOf(jwtAuthorities);
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

    /**
     * Builds a {@link PermissionMatrix} from a bulk DB query for the given authority names.
     * This is only called on Hazelcast cache miss.
     */
    private PermissionMatrix buildMatrix(Collection<String> authorities) {
        List<SecPermission> allPerms = secPermissionRepository.findAllByAuthorityNameIn(authorities);
        return new PermissionMatrix(allPerms);
    }

    /**
     * Derives a deterministic, order-independent cache key from the given authority names.
     * Uses a sorted set so that {@code {ROLE_A, ROLE_B}} and {@code {ROLE_B, ROLE_A}} map to
     * the same cache entry.
     */
    static String toCacheKey(Collection<String> authorities) {
        return new TreeSet<>(authorities).toString();
    }
}
