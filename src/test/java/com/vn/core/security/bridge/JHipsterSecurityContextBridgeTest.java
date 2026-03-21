package com.vn.core.security.bridge;

import static org.assertj.core.api.Assertions.assertThat;

import com.vn.core.security.AuthoritiesConstants;
import java.util.Collection;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class JHipsterSecurityContextBridgeTest {

    private final JHipsterSecurityContextBridge bridge = new JHipsterSecurityContextBridge();

    @BeforeEach
    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserLogin_returnsLoginWhenAuthenticated() {
        setAuthentication("admin", AuthoritiesConstants.ADMIN);
        Optional<String> login = bridge.getCurrentUserLogin();
        assertThat(login).contains("admin");
    }

    @Test
    void getCurrentUserLogin_returnsEmptyWhenNotAuthenticated() {
        Optional<String> login = bridge.getCurrentUserLogin();
        assertThat(login).isEmpty();
    }

    @Test
    void getCurrentUserAuthorities_returnsGrantedAuthorities() {
        setAuthentication("admin", AuthoritiesConstants.ADMIN, AuthoritiesConstants.USER);
        Collection<String> authorities = bridge.getCurrentUserAuthorities();
        assertThat(authorities).containsExactlyInAnyOrder(AuthoritiesConstants.ADMIN, AuthoritiesConstants.USER);
    }

    @Test
    void getCurrentUserAuthorities_returnsEmptyWhenNotAuthenticated() {
        Collection<String> authorities = bridge.getCurrentUserAuthorities();
        assertThat(authorities).isEmpty();
    }

    @Test
    void isAuthenticated_returnsTrueWhenAuthenticated() {
        setAuthentication("user", AuthoritiesConstants.USER);
        assertThat(bridge.isAuthenticated()).isTrue();
    }

    @Test
    void isAuthenticated_returnsFalseWhenNotAuthenticated() {
        assertThat(bridge.isAuthenticated()).isFalse();
    }

    private void setAuthentication(String principal, String... authorities) {
        var ctx = SecurityContextHolder.createEmptyContext();
        var grantedAuthorities = java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(principal, "pw", grantedAuthorities));
        SecurityContextHolder.setContext(ctx);
    }
}
