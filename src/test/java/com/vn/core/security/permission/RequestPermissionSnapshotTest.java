package com.vn.core.security.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.vn.core.security.domain.SecPermission;
import com.vn.core.security.repository.SecPermissionRepository;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

/**
 * Unit tests for {@link RequestPermissionSnapshot} verifying JWT-authority direct trust
 * and PermissionMatrix cache-reuse semantics per D-05, D-06, and D-01.
 *
 * <p>These tests are the Wave 0 anchors for plan 11-01, proving that:
 * <ul>
 *   <li>JWT authority names are returned directly with no {@code jhi_authority} DB lookup</li>
 *   <li>Empty-authority paths return empty collections and {@link PermissionMatrix#EMPTY}</li>
 *   <li>The same request-local snapshot reuses its matrix rather than rebuilding it</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequestPermissionSnapshotTest {

    @Mock
    private SecPermissionRepository secPermissionRepository;

    @Mock
    private HazelcastInstance hazelcastInstance;

    @Mock
    private IMap<String, PermissionMatrix> permissionMatrixCache;

    private RequestPermissionSnapshot snapshot;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(hazelcastInstance.<String, PermissionMatrix>getMap(RequestPermissionSnapshot.PERMISSION_MATRIX_CACHE))
            .thenReturn(permissionMatrixCache);
        snapshot = new RequestPermissionSnapshot(secPermissionRepository, hazelcastInstance);
        SecurityContextHolder.clearContext();
    }

    // --- No authenticated user ---

    @Test
    void getAuthorities_whenNoAuthentication_returnsEmpty() {
        SecurityContextHolder.clearContext();

        Collection<String> authorities = snapshot.getAuthorities();

        assertThat(authorities).isEmpty();
    }

    @Test
    void getMatrix_whenNoAuthentication_returnsEmptyMatrix() {
        SecurityContextHolder.clearContext();

        PermissionMatrix matrix = snapshot.getMatrix();

        assertThat(matrix).isSameAs(PermissionMatrix.EMPTY);
        verify(hazelcastInstance, never()).getMap(RequestPermissionSnapshot.PERMISSION_MATRIX_CACHE);
        verify(secPermissionRepository, never()).findAllByAuthorityNameIn(anyCollection());
    }

    // --- JWT authority names trusted directly ---

    @Test
    void getAuthorities_trustsJwtAuthorityNamesDirectly() {
        setAuthentication("user1", "ROLE_ADMIN", "ROLE_USER");

        Collection<String> authorities = snapshot.getAuthorities();

        assertThat(authorities).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void getAuthorities_singleRole_returnsIt() {
        setAuthentication("user1", "ROLE_VIEWER");

        Collection<String> authorities = snapshot.getAuthorities();

        assertThat(authorities).containsExactly("ROLE_VIEWER");
    }

    @Test
    void getAuthorities_emptyGrantedAuthorities_returnsEmpty() {
        SecurityContext ctx = new SecurityContextImpl();
        ctx.setAuthentication(new TestingAuthenticationToken("user1", null /* no authorities */));
        SecurityContextHolder.setContext(ctx);

        Collection<String> authorities = snapshot.getAuthorities();

        assertThat(authorities).isEmpty();
    }

    // --- No AuthorityRepository lookup on hot path ---

    @Test
    void getAuthorities_doesNotQuerySecPermissionRepository() {
        setAuthentication("user1", "ROLE_ADMIN");

        snapshot.getAuthorities();

        // SecPermissionRepository is never invoked during authority loading — only during matrix build.
        verify(secPermissionRepository, never()).findAllByAuthorityNameIn(anyCollection());
    }

    // --- PermissionMatrix reuse from cross-request Hazelcast cache ---

    @Test
    void getMatrix_usesHazelcastCacheForAuthority() {
        setAuthentication("user1", "ROLE_VIEWER");
        PermissionMatrix cached = new PermissionMatrix(List.of());
        when(permissionMatrixCache.computeIfAbsent(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(
            cached
        );

        PermissionMatrix result = snapshot.getMatrix();

        assertThat(result).isSameAs(cached);
    }

    @Test
    void getMatrix_calledTwice_usesLocalCacheForSecondCall() {
        setAuthentication("user1", "ROLE_VIEWER");
        PermissionMatrix cached = new PermissionMatrix(List.of());
        when(permissionMatrixCache.computeIfAbsent(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(
            cached
        );

        PermissionMatrix first = snapshot.getMatrix();
        PermissionMatrix second = snapshot.getMatrix();

        assertThat(first).isSameAs(second);
        // Hazelcast is only consulted once per request; subsequent calls reuse the request-local cache.
        verify(permissionMatrixCache, times(1)).computeIfAbsent(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void getMatrix_emptyAuthorities_returnsEmptyWithoutCacheCall() {
        SecurityContextHolder.clearContext();

        PermissionMatrix matrix = snapshot.getMatrix();

        assertThat(matrix).isSameAs(PermissionMatrix.EMPTY);
        verify(hazelcastInstance, never()).getMap(RequestPermissionSnapshot.PERMISSION_MATRIX_CACHE);
    }

    @Test
    void getAuthorities_calledTwice_loadsOnlyOnce() {
        setAuthentication("user1", "ROLE_ADMIN");

        Collection<String> first = snapshot.getAuthorities();
        Collection<String> second = snapshot.getAuthorities();

        assertThat(first).isEqualTo(second);
        // SecPermissionRepository is never invoked during authority loading.
        verify(secPermissionRepository, never()).findAllByAuthorityNameIn(anyCollection());
    }

    // --- Cache key determinism ---

    @Test
    void toCacheKey_sameAuthoritiesInDifferentOrder_produceSameKey() {
        String key1 = RequestPermissionSnapshot.toCacheKey(List.of("ROLE_A", "ROLE_B"));
        String key2 = RequestPermissionSnapshot.toCacheKey(List.of("ROLE_B", "ROLE_A"));

        assertThat(key1).isEqualTo(key2);
    }

    @Test
    void toCacheKey_differentAuthoritySets_produceDifferentKeys() {
        String key1 = RequestPermissionSnapshot.toCacheKey(List.of("ROLE_A"));
        String key2 = RequestPermissionSnapshot.toCacheKey(List.of("ROLE_B"));

        assertThat(key1).isNotEqualTo(key2);
    }

    // --- Helpers ---

    private void setAuthentication(String username, String... roles) {
        SimpleGrantedAuthority[] authorities = new SimpleGrantedAuthority[roles.length];
        for (int i = 0; i < roles.length; i++) {
            authorities[i] = new SimpleGrantedAuthority(roles[i]);
        }
        TestingAuthenticationToken auth = new TestingAuthenticationToken(username, null, authorities);
        SecurityContext ctx = new SecurityContextImpl(auth);
        SecurityContextHolder.setContext(ctx);
    }
}
