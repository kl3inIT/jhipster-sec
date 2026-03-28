package com.vn.core.security.bridge;

import com.vn.core.domain.Authority;
import com.vn.core.repository.AuthorityRepository;
import com.vn.core.security.AcceptsGrantedAuthorities;
import com.vn.core.security.permission.RequestPermissionSnapshot;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * {@code @Primary} {@link SecurityContextBridge} implementation that filters out phantom authority
 * names — JWT claims that are not backed by a row in {@code jhi_authority} — before returning
 * authority names to callers.
 * <p>
 * This bean supersedes {@link JHipsterSecurityContextBridge} (which remains as a non-primary
 * fallback). Phase 3 enforcement code should depend on
 * {@link com.vn.core.security.MergedSecurityService} rather than this class directly.
 */
@Primary
@Component
public class MergedSecurityContextBridge implements SecurityContextBridge {

    private final AuthorityRepository authorityRepository;
    private final RequestPermissionSnapshot requestPermissionSnapshot;

    public MergedSecurityContextBridge(AuthorityRepository authorityRepository, RequestPermissionSnapshot requestPermissionSnapshot) {
        this.authorityRepository = authorityRepository;
        this.requestPermissionSnapshot = requestPermissionSnapshot;
    }

    @Override
    public Optional<String> getCurrentUserLogin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        return Optional.ofNullable(auth.getName());
    }

    @Override
    public Collection<String> getCurrentUserAuthorities() {
        // Use request-scoped snapshot when available to avoid repeated DB queries per request.
        if (RequestPermissionSnapshot.isRequestScopeActive()) {
            return requestPermissionSnapshot.getAuthorities();
        }
        // Fallback for non-web contexts (tests, batch, scheduled tasks).
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return List.of();
        }
        Set<String> jwtAuthorities = resolveAuthorities(auth).stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        // D-16: validate against jhi_authority table; drop phantom names not backed by a DB row
        Set<String> validNames = authorityRepository
            .findAllById(jwtAuthorities)
            .stream()
            .map(Authority::getName)
            .collect(Collectors.toSet());
        return jwtAuthorities.stream().filter(validNames::contains).toList();
    }

    @Override
    public boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (
            auth != null &&
            auth.isAuthenticated() &&
            resolveAuthorities(auth)
                .stream()
                .noneMatch(a -> "ROLE_ANONYMOUS".equals(a.getAuthority()))
        );
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
