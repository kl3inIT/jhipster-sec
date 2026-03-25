package com.vn.core.service.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vn.core.security.MergedSecurityService;
import com.vn.core.security.domain.SecNavigationGrant;
import com.vn.core.security.repository.SecNavigationGrantRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CurrentUserNavigationGrantServiceTest {

    private static final String APP_NAME = "jhipster-security-platform";

    @Mock
    private MergedSecurityService mergedSecurityService;

    @Mock
    private SecNavigationGrantRepository secNavigationGrantRepository;

    private CurrentUserNavigationGrantService currentUserNavigationGrantService;

    @BeforeEach
    void setUp() {
        currentUserNavigationGrantService = new CurrentUserNavigationGrantService(mergedSecurityService, secNavigationGrantRepository);
    }

    @Test
    void getAllowedNodeIds_returnsEmptyWhenCurrentUserHasNoAuthorities() {
        when(mergedSecurityService.getCurrentUserAuthorityNames()).thenReturn(List.of());

        List<String> allowedNodeIds = currentUserNavigationGrantService.getAllowedNodeIds(APP_NAME);

        assertThat(allowedNodeIds).isEmpty();
        verifyNoInteractions(secNavigationGrantRepository);
    }

    @Test
    void getAllowedNodeIds_returnsAlphabetizedAllowUnionAcrossAuthorities() {
        when(mergedSecurityService.getCurrentUserAuthorityNames()).thenReturn(List.of("ROLE_ADMIN", "ROLE_USER"));
        when(secNavigationGrantRepository.findAllByAppNameAndAuthorityNameIn(APP_NAME, List.of("ROLE_ADMIN", "ROLE_USER")))
            .thenReturn(
                List.of(
                    grant("ROLE_ADMIN", "security.users", "ALLOW"),
                    grant("ROLE_USER", "entities.employee", "ALLOW"),
                    grant("ROLE_USER", "entities.department", "ALLOW"),
                    grant("ROLE_ADMIN", "entities.employee", "ALLOW")
                )
            );

        List<String> allowedNodeIds = currentUserNavigationGrantService.getAllowedNodeIds(APP_NAME);

        assertThat(allowedNodeIds).containsExactly("entities.department", "entities.employee", "security.users");
    }

    @Test
    void getAllowedNodeIds_excludesDeniedNodeIdsEvenWhenAnotherAuthorityAllowsThem() {
        when(mergedSecurityService.getCurrentUserAuthorityNames()).thenReturn(List.of("ROLE_ADMIN", "ROLE_MANAGER"));
        when(secNavigationGrantRepository.findAllByAppNameAndAuthorityNameIn(APP_NAME, List.of("ROLE_ADMIN", "ROLE_MANAGER")))
            .thenReturn(
                List.of(
                    grant("ROLE_ADMIN", "entities.organization", "ALLOW"),
                    grant("ROLE_MANAGER", "entities.organization", "DENY"),
                    grant("ROLE_ADMIN", "security.roles", "ALLOW")
                )
            );

        List<String> allowedNodeIds = currentUserNavigationGrantService.getAllowedNodeIds(APP_NAME);

        assertThat(allowedNodeIds).containsExactly("security.roles");
    }

    private SecNavigationGrant grant(String authorityName, String nodeId, String effect) {
        return new SecNavigationGrant().authorityName(authorityName).appName(APP_NAME).nodeId(nodeId).effect(effect);
    }
}
