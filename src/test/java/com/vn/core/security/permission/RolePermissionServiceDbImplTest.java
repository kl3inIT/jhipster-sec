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
 * Unit tests for {@link RolePermissionServiceDbImpl} verifying default-deny,
 * union-of-ALLOW semantics, entity wildcard (*) fallback, and correct repository dispatch.
 */
@ExtendWith(MockitoExtension.class)
class RolePermissionServiceDbImplTest {

    @Mock
    private SecPermissionRepository secPermissionRepository;

    @Mock
    private MergedSecurityService mergedSecurityService;

    @Mock
    private RequestPermissionSnapshot requestPermissionSnapshot;

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
        when(secPermissionRepository.findByRolesAndTargets(anyCollection(), any(TargetType.class), any(), any())).thenReturn(
            List.of(allow, deny)
        );

        boolean result = service.hasPermission(List.of("ROLE_USER"), TargetType.ENTITY, "SOMEENTITY", "READ");

        assertThat(result).isTrue();
    }

    @Test
    void testAllowOnlyReturnsTrue() {
        SecPermission allow = new SecPermission().effect("ALLOW");
        when(secPermissionRepository.findByRolesAndTargets(anyCollection(), any(TargetType.class), any(), any())).thenReturn(
            List.of(allow)
        );

        boolean result = service.hasPermission(List.of("ROLE_USER"), TargetType.ENTITY, "SOMEENTITY", "READ");

        assertThat(result).isTrue();
    }

    @Test
    void testEmptyResultReturnsFalse() {
        when(secPermissionRepository.findByRolesAndTargets(anyCollection(), any(TargetType.class), any(), any())).thenReturn(List.of());

        boolean result = service.hasPermission(List.of("ROLE_USER"), TargetType.ENTITY, "SOMEENTITY", "READ");

        assertThat(result).isFalse();
    }

    @Test
    void testEmptyAuthoritiesReturnsFalse() {
        when(mergedSecurityService.getCurrentUserAuthorityNames()).thenReturn(List.of());

        boolean result = service.isEntityOpPermitted(SomeEntity.class, EntityOp.READ);

        assertThat(result).isFalse();
        verify(secPermissionRepository, never()).findByRolesAndTargets(anyCollection(), any(), any(), any());
    }

    @Test
    void testEntityTargetNormalizationIncludesWildcard() {
        when(mergedSecurityService.getCurrentUserAuthorityNames()).thenReturn(List.of("ROLE_USER"));
        when(secPermissionRepository.findByRolesAndTargets(anyCollection(), any(TargetType.class), any(), any())).thenReturn(List.of());

        service.isEntityOpPermitted(SomeEntity.class, EntityOp.READ);

        // Entity checks must include both the specific target and the wildcard "*".
        verify(secPermissionRepository).findByRolesAndTargets(
            anyCollection(),
            eq(TargetType.ENTITY),
            eq(List.of("SOMEENTITY", "*")),
            eq(EntityOp.READ.name())
        );
    }

    @Test
    void testDenyOnlyReturnsFalse() {
        SecPermission deny = new SecPermission().effect("DENY");
        when(secPermissionRepository.findByRolesAndTargets(anyCollection(), any(TargetType.class), any(), any())).thenReturn(List.of(deny));

        boolean result = service.hasPermission(List.of("ROLE_ADMIN"), TargetType.ENTITY, "ORDER", "READ");

        assertThat(result).isFalse();
    }

    @Test
    void testEntityWildcardAllowGrantsAccess() {
        // Wildcard "*" ALLOW in DB grants any entity READ.
        SecPermission wildcardAllow = new SecPermission().effect("ALLOW");
        when(
            secPermissionRepository.findByRolesAndTargets(anyCollection(), eq(TargetType.ENTITY), eq(List.of("ORDER", "*")), eq("READ"))
        ).thenReturn(List.of(wildcardAllow));

        boolean result = service.hasPermission(List.of("ROLE_USER"), TargetType.ENTITY, "ORDER", "READ");

        assertThat(result).isTrue();
    }

    @Test
    void testAttributeCheckDoesNotIncludeWildcardTarget() {
        Collection<String> authorities = List.of("ROLE_ADMIN");
        when(secPermissionRepository.findByRolesAndTargets(anyCollection(), any(TargetType.class), any(), any())).thenReturn(List.of());

        service.hasPermission(authorities, TargetType.ATTRIBUTE, "ORDER.AMOUNT", "VIEW");

        // Attribute type: only the specific target is passed (wildcard handled in AttributePermissionEvaluatorImpl).
        verify(secPermissionRepository).findByRolesAndTargets(authorities, TargetType.ATTRIBUTE, List.of("ORDER.AMOUNT"), "VIEW");
    }
}
