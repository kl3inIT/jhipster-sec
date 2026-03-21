package com.vn.core.security.row;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.vn.core.security.MergedSecurityService;
import com.vn.core.security.domain.SecRowPolicy;
import com.vn.core.security.permission.EntityOp;
import com.vn.core.security.repository.SecRowPolicyRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/**
 * Unit tests for {@link RowLevelPolicyProviderDbImpl} verifying fail-closed behavior per D-13/D-14.
 */
@ExtendWith(MockitoExtension.class)
class RowLevelPolicyProviderDbImplTest {

    @Mock
    private SecRowPolicyRepository secRowPolicyRepository;

    @Mock
    private MergedSecurityService mergedSecurityService;

    @InjectMocks
    private RowLevelPolicyProviderDbImpl provider;

    @Test
    void testSpecificationPolicyWithCurrentUserLogin() {
        SecRowPolicy policy = new SecRowPolicy()
            .code("TEST-01")
            .entityName("SomeEntity")
            .operation("READ")
            .policyType("SPECIFICATION")
            .expression("createdBy = CURRENT_USER_LOGIN");
        when(secRowPolicyRepository.findByEntityNameAndOperation(eq("SomeEntity"), eq("READ"))).thenReturn(List.of(policy));
        when(mergedSecurityService.getCurrentUserLogin()).thenReturn(Optional.of("admin"));

        List<RowPolicyDefinition> policies = provider.getPolicies("SomeEntity", EntityOp.READ);

        assertThat(policies).hasSize(1);
        assertThat(policies.get(0)).isNotNull();
        assertThat(policies.get(0).getSpecification()).isNotNull();
    }

    @Test
    void testUnparseableExpressionFailsClosed() {
        SecRowPolicy policy = new SecRowPolicy()
            .code("INVALID-01")
            .entityName("SomeEntity")
            .operation("READ")
            .policyType("SPECIFICATION")
            .expression("INVALID GARBAGE");
        when(secRowPolicyRepository.findByEntityNameAndOperation(any(), any())).thenReturn(List.of(policy));

        assertThatThrownBy(() -> provider.getPolicies("SomeEntity", EntityOp.READ)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testJavaPolicyTypeFailsClosed() {
        SecRowPolicy policy = new SecRowPolicy()
            .code("JAVA-01")
            .entityName("SomeEntity")
            .operation("READ")
            .policyType("JAVA")
            .expression("someJavaClass.method()");
        when(secRowPolicyRepository.findByEntityNameAndOperation(any(), any())).thenReturn(List.of(policy));

        assertThatThrownBy(() -> provider.getPolicies("SomeEntity", EntityOp.READ)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testNoPoliciesReturnsEmptyList() {
        when(secRowPolicyRepository.findByEntityNameAndOperation(any(), any())).thenReturn(List.of());

        List<RowPolicyDefinition> policies = provider.getPolicies("SomeEntity", EntityOp.READ);

        assertThat(policies).isEmpty();
    }

    @Test
    void testSpecificationPolicyWithUnsupportedTokenFailsClosed() {
        SecRowPolicy policy = new SecRowPolicy()
            .code("UNSUPPORTED-01")
            .entityName("SomeEntity")
            .operation("READ")
            .policyType("SPECIFICATION")
            .expression("createdBy = UNKNOWN_TOKEN");
        when(secRowPolicyRepository.findByEntityNameAndOperation(any(), any())).thenReturn(List.of(policy));

        assertThatThrownBy(() -> provider.getPolicies("SomeEntity", EntityOp.READ)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testUnauthenticatedUserWithSpecPolicyFailsClosed() {
        SecRowPolicy policy = new SecRowPolicy()
            .code("AUTH-REQUIRED-01")
            .entityName("SomeEntity")
            .operation("READ")
            .policyType("SPECIFICATION")
            .expression("createdBy = CURRENT_USER_LOGIN");
        when(secRowPolicyRepository.findByEntityNameAndOperation(any(), any())).thenReturn(List.of(policy));
        when(mergedSecurityService.getCurrentUserLogin()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> provider.getPolicies("SomeEntity", EntityOp.READ)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testJpqlPolicyWithCurrentUserLogin() {
        SecRowPolicy policy = new SecRowPolicy()
            .code("JPQL-01")
            .entityName("SomeEntity")
            .operation("READ")
            .policyType("JPQL")
            .expression("e.createdBy = {CURRENT_USER_LOGIN}");
        when(secRowPolicyRepository.findByEntityNameAndOperation(eq("SomeEntity"), eq("READ"))).thenReturn(List.of(policy));
        when(mergedSecurityService.getCurrentUserLogin()).thenReturn(Optional.of("testuser"));

        List<RowPolicyDefinition> policies = provider.getPolicies("SomeEntity", EntityOp.READ);

        assertThat(policies).hasSize(1);
        assertThat(policies.get(0).getSpecification()).isNotNull();
    }
}
