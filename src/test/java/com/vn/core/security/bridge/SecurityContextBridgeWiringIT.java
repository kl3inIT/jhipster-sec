package com.vn.core.security.bridge;

import static org.assertj.core.api.Assertions.assertThat;

import com.vn.core.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test verifying that the {@code @Primary} {@link SecurityContextBridge} bean is
 * {@link MergedSecurityContextBridge}, and that both bridge implementations coexist in the context.
 * <p>
 * This test also implicitly confirms that the bridge package does not violate ArchUnit layer rules
 * (because {@link com.vn.core.TechnicalStructureTest} runs in the same integration test suite).
 */
@IntegrationTest
class SecurityContextBridgeWiringIT {

    @Autowired
    private SecurityContextBridge bridge;

    @Autowired
    private List<SecurityContextBridge> allBridges;

    @Test
    void primaryBridgeIsWired() {
        assertThat(bridge).isNotNull();
        assertThat(bridge).isInstanceOf(MergedSecurityContextBridge.class);
    }

    @Test
    void testBothBridgeBeansExist() {
        assertThat(allBridges).hasSizeGreaterThanOrEqualTo(2);
        assertThat(allBridges).anyMatch(b -> b instanceof MergedSecurityContextBridge);
        assertThat(allBridges).anyMatch(b -> b instanceof JHipsterSecurityContextBridge);
    }

    @Test
    void bridgeReturnsEmptyWhenNoSecurityContext() {
        assertThat(bridge.getCurrentUserLogin()).isEmpty();
        assertThat(bridge.getCurrentUserAuthorities()).isEmpty();
        assertThat(bridge.isAuthenticated()).isFalse();
    }
}
