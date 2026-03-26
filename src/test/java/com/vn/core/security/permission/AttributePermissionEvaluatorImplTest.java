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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link AttributePermissionEvaluatorImpl} verifying deny-default
 * and union-of-ALLOW semantics for attribute-level checks.
 */
@ExtendWith(MockitoExtension.class)
class AttributePermissionEvaluatorImplTest {

    @Mock
    private SecPermissionRepository secPermissionRepository;

    @Mock
    private MergedSecurityService mergedSecurityService;

    @InjectMocks
    private AttributePermissionEvaluatorImpl evaluator;

    static class SomeEntity {

        Long id;
        String name;
        String secret;
    }

    @Test
    void testEmptyResultReturnsFalse() {
        when(mergedSecurityService.getCurrentUserAuthorityNames()).thenReturn(List.of("ROLE_USER"));
        when(secPermissionRepository.findByRolesAndTargets(anyCollection(), any(TargetType.class), anyCollection(), any())).thenReturn(
            List.of()
        );

        boolean result = evaluator.canView(SomeEntity.class, "name");

        assertThat(result).isFalse();
    }

    @Test
    void testAllowOnlyReturnsTrue() {
        when(mergedSecurityService.getCurrentUserAuthorityNames()).thenReturn(List.of("ROLE_USER"));
        SecPermission allow = new SecPermission().effect("ALLOW");
        when(secPermissionRepository.findByRolesAndTargets(anyCollection(), any(TargetType.class), anyCollection(), any())).thenReturn(
            List.of(allow)
        );

        boolean result = evaluator.canView(SomeEntity.class, "name");

        assertThat(result).isTrue();
    }

    @Test
    void testDenyOnlyReturnsFalse() {
        when(mergedSecurityService.getCurrentUserAuthorityNames()).thenReturn(List.of("ROLE_USER"));
        SecPermission deny = new SecPermission().effect("DENY");
        when(secPermissionRepository.findByRolesAndTargets(anyCollection(), any(TargetType.class), anyCollection(), any())).thenReturn(
            List.of(deny)
        );

        boolean result = evaluator.canView(SomeEntity.class, "secret");

        assertThat(result).isFalse();
    }

    @Test
    void testTargetNormalizationIncludesWildcardTarget() {
        when(mergedSecurityService.getCurrentUserAuthorityNames()).thenReturn(List.of("ROLE_USER"));
        when(secPermissionRepository.findByRolesAndTargets(anyCollection(), any(TargetType.class), anyCollection(), any())).thenReturn(
            List.of()
        );

        evaluator.canView(SomeEntity.class, "myField");

        verify(secPermissionRepository).findByRolesAndTargets(
            anyCollection(),
            eq(TargetType.ATTRIBUTE),
            eq(List.of("SOMEENTITY.MYFIELD", "SOMEENTITY.*")),
            eq("VIEW")
        );
    }

    @Test
    void testEmptyAuthoritiesReturnsFalse() {
        when(mergedSecurityService.getCurrentUserAuthorityNames()).thenReturn(List.of());

        boolean result = evaluator.canView(SomeEntity.class, "name");

        assertThat(result).isFalse();
        verify(secPermissionRepository, never()).findByRolesAndTargets(anyCollection(), any(TargetType.class), anyCollection(), any());
    }

    @Test
    void testCanEditWithAllowReturnsTrue() {
        when(mergedSecurityService.getCurrentUserAuthorityNames()).thenReturn(List.of("ROLE_ADMIN"));
        SecPermission allow = new SecPermission().effect("ALLOW");
        when(secPermissionRepository.findByRolesAndTargets(anyCollection(), any(TargetType.class), anyCollection(), any())).thenReturn(
            List.of(allow)
        );

        boolean result = evaluator.canEdit(SomeEntity.class, "name");

        assertThat(result).isTrue();
    }

    @Test
    void testAllowAndDenyStillReturnsTrue() {
        when(mergedSecurityService.getCurrentUserAuthorityNames()).thenReturn(List.of("ROLE_USER", "ROLE_ADMIN"));
        SecPermission allow = new SecPermission().effect("ALLOW");
        SecPermission deny = new SecPermission().effect("DENY");
        when(secPermissionRepository.findByRolesAndTargets(anyCollection(), any(TargetType.class), anyCollection(), any())).thenReturn(
            List.of(allow, deny)
        );

        boolean result = evaluator.canView(SomeEntity.class, "name");

        assertThat(result).isTrue();
    }

    @Test
    void testWildcardAllowReturnsTrue() {
        when(mergedSecurityService.getCurrentUserAuthorityNames()).thenReturn(List.of("ROLE_ADMIN"));
        SecPermission wildcardAllow = new SecPermission().effect("ALLOW").target("SOMEENTITY.*");
        when(secPermissionRepository.findByRolesAndTargets(anyCollection(), any(TargetType.class), anyCollection(), any())).thenReturn(
            List.of(wildcardAllow)
        );

        boolean result = evaluator.canEdit(SomeEntity.class, "name");

        assertThat(result).isTrue();
    }
}
