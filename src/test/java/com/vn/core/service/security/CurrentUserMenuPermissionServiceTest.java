package com.vn.core.service.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vn.core.security.MergedSecurityService;
import com.vn.core.security.domain.SecMenuPermission;
import com.vn.core.security.repository.SecMenuPermissionRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CurrentUserMenuPermissionServiceTest {

    private static final String APP_NAME = "jhipster-security-platform";

    @Mock
    private MergedSecurityService mergedSecurityService;

    @Mock
    private SecMenuPermissionRepository secMenuPermissionRepository;

    private CurrentUserMenuPermissionService currentUserMenuPermissionService;

    @BeforeEach
    void setUp() {
        currentUserMenuPermissionService = new CurrentUserMenuPermissionService(mergedSecurityService, secMenuPermissionRepository);
    }

    @Test
    void getAllowedMenuIds_returnsEmptyWhenCurrentUserHasNoAuthorities() {
        when(mergedSecurityService.getCurrentUserAuthorityNames()).thenReturn(List.of());

        List<String> allowedMenuIds = currentUserMenuPermissionService.getAllowedMenuIds(APP_NAME);

        assertThat(allowedMenuIds).isEmpty();
        verifyNoInteractions(secMenuPermissionRepository);
    }

    @Test
    void allowsMenuWhenAnyAuthorityAllowsIt() {
        when(mergedSecurityService.getCurrentUserAuthorityNames()).thenReturn(List.of("ROLE_ADMIN", "ROLE_USER"));
        when(secMenuPermissionRepository.findAllByAppNameAndRoleIn(APP_NAME, List.of("ROLE_ADMIN", "ROLE_USER"))).thenReturn(
            List.of(
                permission("ROLE_ADMIN", "security.users", "ALLOW"),
                permission("ROLE_USER", "entities.employee", "ALLOW"),
                permission("ROLE_USER", "entities.department", "ALLOW"),
                permission("ROLE_ADMIN", "entities.employee", "ALLOW")
            )
        );

        List<String> allowedMenuIds = currentUserMenuPermissionService.getAllowedMenuIds(APP_NAME);

        assertThat(allowedMenuIds).containsExactly("entities.department", "entities.employee", "security.users");
    }

    @Test
    void allowsMenuWhenAnyAuthorityAllowsIt_evenIfAnotherAuthorityDeniesTheSameMenu() {
        when(mergedSecurityService.getCurrentUserAuthorityNames()).thenReturn(List.of("ROLE_ADMIN", "ROLE_MANAGER"));
        when(secMenuPermissionRepository.findAllByAppNameAndRoleIn(APP_NAME, List.of("ROLE_ADMIN", "ROLE_MANAGER"))).thenReturn(
            List.of(
                permission("ROLE_ADMIN", "entities.organization", "ALLOW"),
                permission("ROLE_MANAGER", "entities.organization", "DENY"),
                permission("ROLE_ADMIN", "security.roles", "ALLOW")
            )
        );

        List<String> allowedMenuIds = currentUserMenuPermissionService.getAllowedMenuIds(APP_NAME);

        assertThat(allowedMenuIds).containsExactly("entities.organization", "security.roles");
    }

    private SecMenuPermission permission(String role, String menuId, String effect) {
        return new SecMenuPermission().role(role).appName(APP_NAME).menuId(menuId).effect(effect);
    }
}
