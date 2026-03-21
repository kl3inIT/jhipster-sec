package com.vn.core.security.bridge;

import static org.assertj.core.api.Assertions.assertThat;

import com.vn.core.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test verifying that the default {@link SecurityContextBridge} bean is wired
 * and is an instance of {@link JHipsterSecurityContextBridge}.
 * <p>
 * This test also implicitly confirms that the bridge package does not violate ArchUnit layer rules
 * (because {@link com.vn.core.TechnicalStructureTest} runs in the same integration test suite).
 */
@IntegrationTest
class SecurityContextBridgeWiringIT {

    @Autowired
    private SecurityContextBridge bridge;

    @Test
    void defaultBridgeIsWired() {
        assertThat(bridge).isNotNull();
        assertThat(bridge).isInstanceOf(JHipsterSecurityContextBridge.class);
    }

    @Test
    void bridgeReturnsEmptyWhenNoSecurityContext() {
        assertThat(bridge.getCurrentUserLogin()).isEmpty();
        assertThat(bridge.getCurrentUserAuthorities()).isEmpty();
        assertThat(bridge.isAuthenticated()).isFalse();
    }
}
