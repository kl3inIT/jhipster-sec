package com.vn.core.security.bridge;

import com.vn.core.security.SecurityUtils;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Default {@link SecurityContextBridge} implementation backed by JHipster's SecurityContext.
 * <p>
 * This bean is a plain {@code @Component} without {@code @Primary}, so Phase 2 can override it
 * by declaring a {@code @Primary} bean implementing the same interface.
 */
@Component
public class JHipsterSecurityContextBridge implements SecurityContextBridge {

    @Override
    public Optional<String> getCurrentUserLogin() {
        return SecurityUtils.getCurrentUserLogin();
    }

    @Override
    public Collection<String> getCurrentUserAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return List.of();
        }
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }

    @Override
    public boolean isAuthenticated() {
        return SecurityUtils.isAuthenticated();
    }
}
