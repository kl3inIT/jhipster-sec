package com.vn.core.security;

import com.vn.core.security.bridge.SecurityContextBridge;
import java.util.Collection;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link MergedSecurityService}.
 * Delegates to the injected {@link SecurityContextBridge}. Because
 * {@link com.vn.core.security.bridge.MergedSecurityContextBridge} is {@code @Primary},
 * Spring will inject it here, giving Phase 3 callers phantom-filtered authority names.
 */
@Service
public class MergedSecurityServiceImpl implements MergedSecurityService {

    private final SecurityContextBridge securityContextBridge;

    public MergedSecurityServiceImpl(SecurityContextBridge securityContextBridge) {
        this.securityContextBridge = securityContextBridge;
    }

    @Override
    public Collection<String> getCurrentUserAuthorityNames() {
        return securityContextBridge.getCurrentUserAuthorities();
    }

    @Override
    public Optional<String> getCurrentUserLogin() {
        return securityContextBridge.getCurrentUserLogin();
    }

    @Override
    public boolean isAuthenticated() {
        return securityContextBridge.isAuthenticated();
    }
}
