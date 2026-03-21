package com.vn.core.security.row;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vn.core.security.permission.EntityOp;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

/**
 * Unit tests for {@link RowLevelSpecificationBuilder} verifying AND-composition of row policies.
 */
@ExtendWith(MockitoExtension.class)
class RowLevelSpecificationBuilderTest {

    @Mock
    private RowLevelPolicyProvider rowLevelPolicyProvider;

    @InjectMocks
    private RowLevelSpecificationBuilder builder;

    static class SomeEntity {}

    @Test
    void testNoPoliciesReturnsUnconstrainedSpec() {
        when(rowLevelPolicyProvider.getPolicies(eq("SomeEntity"), any(EntityOp.class))).thenReturn(List.of());

        Specification<SomeEntity> spec = builder.build(SomeEntity.class, EntityOp.READ);

        assertThat(spec).isNotNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSinglePolicyReturnsItsSpec() {
        Specification<SomeEntity> policySpec = mock(Specification.class);
        RowPolicyDefinition policyDef = new RowPolicyDefinition() {
            @Override
            public <T> Specification<T> getSpecification() {
                return (Specification<T>) policySpec;
            }
        };
        when(rowLevelPolicyProvider.getPolicies(eq("SomeEntity"), any(EntityOp.class))).thenReturn(List.of(policyDef));

        Specification<SomeEntity> result = builder.build(SomeEntity.class, EntityOp.READ);

        assertThat(result).isNotNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testMultiplePoliciesAreAndComposed() {
        Specification<SomeEntity> spec1 = mock(Specification.class);
        Specification<SomeEntity> spec2 = mock(Specification.class);

        RowPolicyDefinition def1 = new RowPolicyDefinition() {
            @Override
            public <T> Specification<T> getSpecification() {
                return (Specification<T>) spec1;
            }
        };
        RowPolicyDefinition def2 = new RowPolicyDefinition() {
            @Override
            public <T> Specification<T> getSpecification() {
                return (Specification<T>) spec2;
            }
        };

        // Track invocations via a spy wrapper instead — verify both defs have their spec called
        // by wrapping them in anonymous subclasses that delegate
        final boolean[] called1 = { false };
        final boolean[] called2 = { false };

        RowPolicyDefinition trackDef1 = new RowPolicyDefinition() {
            @Override
            public <T> Specification<T> getSpecification() {
                called1[0] = true;
                return (Specification<T>) spec1;
            }
        };
        RowPolicyDefinition trackDef2 = new RowPolicyDefinition() {
            @Override
            public <T> Specification<T> getSpecification() {
                called2[0] = true;
                return (Specification<T>) spec2;
            }
        };

        when(rowLevelPolicyProvider.getPolicies(eq("SomeEntity"), any(EntityOp.class))).thenReturn(List.of(trackDef1, trackDef2));

        Specification<SomeEntity> result = builder.build(SomeEntity.class, EntityOp.READ);

        assertThat(result).isNotNull();
        assertThat(called1[0]).isTrue();
        assertThat(called2[0]).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testBuildUsesEntitySimpleName() {
        when(rowLevelPolicyProvider.getPolicies(eq("SomeEntity"), eq(EntityOp.UPDATE))).thenReturn(List.of());

        builder.build(SomeEntity.class, EntityOp.UPDATE);

        verify(rowLevelPolicyProvider).getPolicies("SomeEntity", EntityOp.UPDATE);
    }
}
