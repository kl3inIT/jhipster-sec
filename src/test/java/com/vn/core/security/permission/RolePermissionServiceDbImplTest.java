package com.vn.core.security.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vn.core.security.MergedSecurityService;
import com.vn.core.security.domain.SecPermission;
import com.vn.core.security.repository.SecPermissionRepository;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link RolePermissionServiceDbImpl} verifying default-deny
 * union-of-ALLOW semantics for entity checks.
 */
@ExtendWith(MockitoExtension.class)
class RolePermissionServiceDbImplTest {

    @Mock
    private SecPermissionRepository secPermissionRepository;

    @Mock
    private MergedSecurityService mergedSecurityService;

    @InjectMocks
    private RolePermissionServiceDbImpl service;

    /** Simple entity class used for target normalization tests. */
    static class SomeEntity {

        Long id;
    }

    @Test
    void testAllowAndDenyStillReturnsTrue() {
        SecPermission allow = new SecPermission().effect("ALLOW");
        SecPermission deny = new SecPermission().effect("DENY");
        when(secPermissionRepository.findByRolesAndTarget(anyCollection(), any(TargetType.class), any(), any())).thenReturn(
            List.of(allow, deny)
        );

        boolean result = service.hasPermission(List.of("ROLE_USER"), TargetType.ENTITY, "SOMEENTITY", "READ");

        assertThat(result).isTrue();
    }

    @Test
    void testAllowOnlyReturnsTrue() {
        SecPermission allow = new SecPermission().effect("ALLOW");
        when(secPermissionRepository.findByRolesAndTarget(anyCollection(), any(TargetType.class), any(), any())).thenReturn(List.of(allow));

        boolean result = service.hasPermission(List.of("ROLE_USER"), TargetType.ENTITY, "SOMEENTITY", "READ");

        assertThat(result).isTrue();
    }

    @Test
    void testEmptyResultReturnsFalse() {
        when(secPermissionRepository.findByRolesAndTarget(anyCollection(), any(TargetType.class), any(), any())).thenReturn(List.of());

        boolean result = service.hasPermission(List.of("ROLE_USER"), TargetType.ENTITY, "SOMEENTITY", "READ");

        assertThat(result).isFalse();
    }

    @Test
    void testEmptyAuthoritiesReturnsFalse() {
        when(mergedSecurityService.getCurrentUserAuthorityNames()).thenReturn(List.of());

        boolean result = service.isEntityOpPermitted(SomeEntity.class, EntityOp.READ);

        assertThat(result).isFalse();
        verify(secPermissionRepository, never()).findByRolesAndTarget(anyCollection(), any(), any(), any());
    }

    @Test
    void testEntityTargetNormalization() {
        when(mergedSecurityService.getCurrentUserAuthorityNames()).thenReturn(List.of("ROLE_USER"));
        when(secPermissionRepository.findByRolesAndTarget(anyCollection(), any(TargetType.class), eq("SOMEENTITY"), any())).thenReturn(
            List.of()
        );

        service.isEntityOpPermitted(SomeEntity.class, EntityOp.READ);

        verify(secPermissionRepository).findByRolesAndTarget(
            anyCollection(),
            eq(TargetType.ENTITY),
            eq("SOMEENTITY"),
            eq(EntityOp.READ.name())
        );
    }

    @Test
    void testDenyOnlyReturnsFalse() {
        SecPermission deny = new SecPermission().effect("DENY");
        when(secPermissionRepository.findByRolesAndTarget(anyCollection(), any(TargetType.class), any(), any())).thenReturn(List.of(deny));

        boolean result = service.hasPermission(List.of("ROLE_ADMIN"), TargetType.ENTITY, "ORDER", "READ");

        assertThat(result).isFalse();
    }

    @Test
    void testHasPermissionCallsRepositoryWithCorrectParams() {
        Collection<String> authorities = List.of("ROLE_ADMIN");
        when(secPermissionRepository.findByRolesAndTarget(anyCollection(), any(TargetType.class), any(), any())).thenReturn(List.of());

        service.hasPermission(authorities, TargetType.ATTRIBUTE, "ORDER.AMOUNT", "VIEW");

        verify(secPermissionRepository).findByRolesAndTarget(authorities, TargetType.ATTRIBUTE, "ORDER.AMOUNT", "VIEW");
    }
}
