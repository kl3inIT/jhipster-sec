package com.vn.core.security.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vn.core.security.access.AccessManager;
import com.vn.core.security.access.CrudEntityContext;
import com.vn.core.security.permission.EntityOp;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

/**
 * Unit tests for {@link DataManagerImpl} verifying CRUD enforcement stays centralized
 * before delegating to the unconstrained mechanical core.
 */
@ExtendWith(MockitoExtension.class)
class DataManagerImplTest {

    @Mock
    private AccessManager accessManager;

    @Mock
    private UnconstrainedDataManager unconstrainedDataManager;

    @InjectMocks
    private DataManagerImpl dataManager;

    static class TestEntity {}

    @Test
    void testLoadPageDeniedCrudThrowsBeforeUnconstrainedAccess() {
        when(accessManager.applyRegisteredConstraints(any(CrudEntityContext.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Specification<TestEntity> spec = (root, query, cb) -> cb.conjunction();
        PageRequest pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> dataManager.loadPage(TestEntity.class, spec, pageable, EntityOp.READ)).isInstanceOf(
            AccessDeniedException.class
        );

        verifyNoInteractions(unconstrainedDataManager);
    }

    @Test
    void testLoadPageDelegatesToUnconstrainedDataManagerAfterCrudCheck() {
        when(accessManager.applyRegisteredConstraints(any(CrudEntityContext.class))).thenAnswer(invocation -> {
            CrudEntityContext ctx = invocation.getArgument(0);
            ctx.setPermitted(true);
            return ctx;
        });

        Specification<TestEntity> spec = (root, query, cb) -> cb.conjunction();
        PageRequest pageable = PageRequest.of(0, 10);
        Page<TestEntity> expected = new PageImpl<>(List.of(new TestEntity()), pageable, 1);
        when(unconstrainedDataManager.loadPage(TestEntity.class, spec, pageable)).thenReturn(expected);

        Page<TestEntity> result = dataManager.loadPage(TestEntity.class, spec, pageable, EntityOp.READ);

        assertThat(result).isSameAs(expected);
        verify(accessManager).applyRegisteredConstraints(any(CrudEntityContext.class));
        verify(unconstrainedDataManager).loadPage(TestEntity.class, spec, pageable);
    }

    @Test
    void testUnconstrainedReturnsInjectedBypassObject() {
        assertThat(dataManager.unconstrained()).isSameAs(unconstrainedDataManager);
    }
}
