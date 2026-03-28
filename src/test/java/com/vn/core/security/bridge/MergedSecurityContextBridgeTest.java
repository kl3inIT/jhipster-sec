package com.vn.core.security.bridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import com.vn.core.domain.Authority;
import com.vn.core.repository.AuthorityRepository;
import com.vn.core.security.permission.RequestPermissionSnapshot;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class MergedSecurityContextBridgeTest {

    @Mock
    private AuthorityRepository authorityRepository;

    @Mock
    private RequestPermissionSnapshot requestPermissionSnapshot;

    private MergedSecurityContextBridge bridge;

    @BeforeEach
    void setUp() {
        // In unit tests there is no active request context, so isRequestScopeActive() returns false
        // and the bridge falls back to direct authorityRepository queries — snapshot is not called.
        bridge = new MergedSecurityContextBridge(authorityRepository, requestPermissionSnapshot);
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetCurrentUserAuthorities_filtersPhantomRoles() {
        setAuthentication("admin", "ROLE_ADMIN", "ROLE_PHANTOM");

        Authority adminAuthority = new Authority();
        adminAuthority.setName("ROLE_ADMIN");
        adminAuthority.setIsPersisted();

        when(authorityRepository.findAllById(anyCollection())).thenReturn(List.of(adminAuthority));

        Collection<String> result = bridge.getCurrentUserAuthorities();

        assertThat(result).containsExactly("ROLE_ADMIN");
        assertThat(result).doesNotContain("ROLE_PHANTOM");
    }

    @Test
    void testGetCurrentUserAuthorities_emptyWhenUnauthenticated() {
        Collection<String> result = bridge.getCurrentUserAuthorities();
        assertThat(result).isEmpty();
    }

    @Test
    void testGetCurrentUserAuthorities_returnsAllWhenAllValid() {
        setAuthentication("admin", "ROLE_ADMIN", "ROLE_USER");

        Authority adminAuthority = new Authority();
        adminAuthority.setName("ROLE_ADMIN");
        adminAuthority.setIsPersisted();

        Authority userAuthority = new Authority();
        userAuthority.setName("ROLE_USER");
        userAuthority.setIsPersisted();

        when(authorityRepository.findAllById(anyCollection())).thenReturn(List.of(adminAuthority, userAuthority));

        Collection<String> result = bridge.getCurrentUserAuthorities();

        assertThat(result).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void testGetCurrentUserLogin_returnsLogin() {
        setAuthentication("admin", "ROLE_ADMIN");

        Optional<String> login = bridge.getCurrentUserLogin();

        assertThat(login).contains("admin");
    }

    @Test
    void testGetCurrentUserLogin_emptyWhenUnauthenticated() {
        Optional<String> login = bridge.getCurrentUserLogin();
        assertThat(login).isEmpty();
    }

    @Test
    void testIsAuthenticated_trueForAuthenticatedUser() {
        setAuthentication("admin", "ROLE_ADMIN");

        assertThat(bridge.isAuthenticated()).isTrue();
    }

    @Test
    void testIsAuthenticated_falseForAnonymous() {
        setAuthentication("anonymous", "ROLE_ANONYMOUS");

        assertThat(bridge.isAuthenticated()).isFalse();
    }

    private void setAuthentication(String principal, String... authorities) {
        var ctx = SecurityContextHolder.createEmptyContext();
        var grantedAuthorities = java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(principal, "pw", grantedAuthorities));
        SecurityContextHolder.setContext(ctx);
    }
}
