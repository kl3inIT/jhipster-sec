package com.vn.core.security.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vn.core.security.domain.SecPermission;
import com.vn.core.security.repository.SecPermissionRepository;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class RequestPermissionSnapshotTest {

    @Mock
    private SecPermissionRepository secPermissionRepository;

    private RequestPermissionSnapshot snapshot;

    @BeforeEach
    void setUp() {
        snapshot = new RequestPermissionSnapshot(secPermissionRepository);
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

    // --- No AuthorityRepository lookup ---

    @Test
    void getAuthorities_doesNotQueryAuthorityRepository() {
        setAuthentication("user1", "ROLE_ADMIN");

        snapshot.getAuthorities();

        // No AuthorityRepository is injected into this constructor; confirm only
        // SecPermissionRepository can be called (and only on getMatrix, not getAuthorities).
        verify(secPermissionRepository, never()).findAllByAuthorityNameIn(anyCollection());
    }

    // --- PermissionMatrix cache reuse within same snapshot instance ---

    @Test
    void getMatrix_calledTwice_buildsOnlyOnce() {
        setAuthentication("user1", "ROLE_VIEWER");
        when(secPermissionRepository.findAllByAuthorityNameIn(anyCollection())).thenReturn(List.of());

        PermissionMatrix first = snapshot.getMatrix();
        PermissionMatrix second = snapshot.getMatrix();

        assertThat(first).isSameAs(second);
        verify(secPermissionRepository, times(1)).findAllByAuthorityNameIn(anyCollection());
    }

    @Test
    void getMatrix_withPermissions_buildsMatrixFromRepository() {
        setAuthentication("user1", "ROLE_VIEWER");
        SecPermission perm = new SecPermission()
            .authorityName("ROLE_VIEWER")
            .targetType(TargetType.ENTITY)
            .target("ORGANIZATION")
            .action("READ")
            .effect("ALLOW");
        when(secPermissionRepository.findAllByAuthorityNameIn(anyCollection())).thenReturn(List.of(perm));

        PermissionMatrix matrix = snapshot.getMatrix();

        assertThat(matrix.isEntityPermitted("ORGANIZATION", "READ")).isTrue();
    }

    @Test
    void getMatrix_emptyAuthorities_returnsEmptyWithoutRepositoryCall() {
        SecurityContextHolder.clearContext();

        PermissionMatrix matrix = snapshot.getMatrix();

        assertThat(matrix).isSameAs(PermissionMatrix.EMPTY);
        verify(secPermissionRepository, never()).findAllByAuthorityNameIn(anyCollection());
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
