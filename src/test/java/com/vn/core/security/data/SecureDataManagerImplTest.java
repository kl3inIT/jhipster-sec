package com.vn.core.security.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vn.core.security.access.AccessManager;
import com.vn.core.security.access.CrudEntityContext;
import com.vn.core.security.catalog.SecuredEntityCatalog;
import com.vn.core.security.catalog.SecuredEntityEntry;
import com.vn.core.security.fetch.FetchPlan;
import com.vn.core.security.fetch.FetchPlanBuilder;
import com.vn.core.security.fetch.FetchPlanResolver;
import com.vn.core.security.merge.SecureMergeService;
import com.vn.core.security.permission.EntityOp;
import com.vn.core.security.repository.RepositoryRegistry;
import com.vn.core.security.row.RowLevelSpecificationBuilder;
import com.vn.core.security.serialize.SecureEntitySerializer;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.security.access.AccessDeniedException;

/**
 * Unit tests for {@link SecureDataManagerImpl} verifying the enforcement pipeline order
 * per D-05/D-06 and fail-closed behavior.
 */
@ExtendWith(MockitoExtension.class)
class SecureDataManagerImplTest {

    @Mock
    private AccessManager accessManager;

    @Mock
    private SecuredEntityCatalog catalog;

    @Mock
    private RowLevelSpecificationBuilder rowLevelSpecificationBuilder;

    @Mock
    private FetchPlanResolver fetchPlanResolver;

    @Mock
    private SecureEntitySerializer secureEntitySerializer;

    @Mock
    private SecureMergeService secureMergeService;

    @Mock
    private RepositoryRegistry repositoryRegistry;

    @InjectMocks
    private SecureDataManagerImpl dataManager;

    static class TestEntity {

        private Long id;

        public TestEntity() {}

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    private SecuredEntityEntry testEntry;
    private FetchPlan testFetchPlan;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        testEntry = SecuredEntityEntry.builder()
            .entityClass(TestEntity.class)
            .code("TEST_ENTITY")
            .operations(EnumSet.allOf(EntityOp.class))
            .fetchPlanCodes(List.of("base"))
            .jpqlAllowed(false)
            .build();

        testFetchPlan = new FetchPlanBuilder(TestEntity.class, "base").add("id").build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testLoadByQueryEnforcesReadOrder() {
        when(catalog.findByCode("TEST_ENTITY")).thenReturn(Optional.of(testEntry));
        doAnswer(invoc -> {
            CrudEntityContext ctx = invoc.getArgument(0);
            ctx.setPermitted(true);
            return ctx;
        })
            .when(accessManager)
            .applyRegisteredConstraints(any(CrudEntityContext.class));

        Specification<Object> mockSpec = (root, query, cb) -> null;
        when(rowLevelSpecificationBuilder.build(eq(TestEntity.class), eq(EntityOp.READ))).thenReturn((Specification) mockSpec);

        JpaSpecificationExecutor<Object> specRepo = mock(JpaSpecificationExecutor.class);
        when(repositoryRegistry.getSpecificationExecutor(TestEntity.class)).thenReturn((JpaSpecificationExecutor) specRepo);

        TestEntity entity = new TestEntity();
        Page<Object> page = new PageImpl<>(List.of(entity));
        when(specRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        when(fetchPlanResolver.resolve(TestEntity.class, "base")).thenReturn(testFetchPlan);
        when(secureEntitySerializer.serialize(any(), any())).thenReturn(Map.of("id", 1L));

        Pageable pageable = PageRequest.of(0, 10);
        SecuredLoadQuery query = SecuredLoadQuery.of("TEST_ENTITY", "base", pageable);
        Page<Map<String, Object>> result = dataManager.loadByQuery(query);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        // Verify enforcement order: accessManager -> rowLevelSpec -> specRepo -> fetchPlanResolver -> serializer
        InOrder order = inOrder(accessManager, rowLevelSpecificationBuilder, specRepo, fetchPlanResolver, secureEntitySerializer);
        order.verify(accessManager).applyRegisteredConstraints(any(CrudEntityContext.class));
        order.verify(rowLevelSpecificationBuilder).build(any(), any());
        order.verify(specRepo).findAll(any(Specification.class), any(Pageable.class));
        order.verify(fetchPlanResolver).resolve(any(), any());
        order.verify(secureEntitySerializer).serialize(any(), any());
    }

    @Test
    void testLoadByQueryDeniedCrudThrows() {
        when(catalog.findByCode("TEST_ENTITY")).thenReturn(Optional.of(testEntry));
        doAnswer(invoc -> {
            // permitted stays false — simulates CRUD denial
            return invoc.getArgument(0);
        })
            .when(accessManager)
            .applyRegisteredConstraints(any(CrudEntityContext.class));

        Pageable pageable = PageRequest.of(0, 10);
        SecuredLoadQuery query = SecuredLoadQuery.of("TEST_ENTITY", "base", pageable);

        assertThatThrownBy(() -> dataManager.loadByQuery(query)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSaveEnforcesWriteOrder() {
        when(catalog.findByCode("TEST_ENTITY")).thenReturn(Optional.of(testEntry));
        doAnswer(invoc -> {
            CrudEntityContext ctx = invoc.getArgument(0);
            ctx.setPermitted(true);
            return ctx;
        })
            .when(accessManager)
            .applyRegisteredConstraints(any(CrudEntityContext.class));

        JpaRepository<Object, Object> repo = mock(JpaRepository.class);
        when(repositoryRegistry.getRepository(TestEntity.class)).thenReturn((JpaRepository) repo);

        TestEntity saved = new TestEntity();
        when(repo.save(any())).thenReturn(saved);
        when(fetchPlanResolver.resolve(TestEntity.class, "base")).thenReturn(testFetchPlan);
        when(secureEntitySerializer.serialize(any(), any())).thenReturn(Map.of("id", 1L));

        // null id = CREATE
        Map<String, Object> result = dataManager.save("TEST_ENTITY", null, Map.of("name", "test"), "base");

        assertThat(result).isNotNull();

        // Verify order: CRUD check -> merge -> repo.save -> fetchPlanResolver -> serializer
        InOrder order = inOrder(accessManager, secureMergeService, repo, fetchPlanResolver, secureEntitySerializer);
        order.verify(accessManager).applyRegisteredConstraints(any(CrudEntityContext.class));
        order.verify(secureMergeService).mergeForUpdate(any(), any());
        order.verify(repo).save(any());
        order.verify(fetchPlanResolver).resolve(any(), any());
        order.verify(secureEntitySerializer).serialize(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDeleteEnforcesDeleteOrder() {
        when(catalog.findByCode("TEST_ENTITY")).thenReturn(Optional.of(testEntry));
        doAnswer(invoc -> {
            CrudEntityContext ctx = invoc.getArgument(0);
            ctx.setPermitted(true);
            return ctx;
        })
            .when(accessManager)
            .applyRegisteredConstraints(any(CrudEntityContext.class));

        Specification<Object> mockSpec = (root, query, cb) -> null;
        when(rowLevelSpecificationBuilder.build(eq(TestEntity.class), eq(EntityOp.DELETE))).thenReturn((Specification) mockSpec);

        JpaSpecificationExecutor<Object> specRepo = mock(JpaSpecificationExecutor.class);
        when(repositoryRegistry.getSpecificationExecutor(TestEntity.class)).thenReturn((JpaSpecificationExecutor) specRepo);

        TestEntity entity = new TestEntity();
        when(specRepo.findOne(any(Specification.class))).thenReturn(Optional.of(entity));

        JpaRepository<Object, Object> repo = mock(JpaRepository.class);
        when(repositoryRegistry.getRepository(TestEntity.class)).thenReturn((JpaRepository) repo);

        dataManager.delete("TEST_ENTITY", 1L);

        // Verify order: CRUD check -> rowLevelSpec -> specRepo.findOne -> repo.delete
        InOrder order = inOrder(accessManager, rowLevelSpecificationBuilder, specRepo, repo);
        order.verify(accessManager).applyRegisteredConstraints(any(CrudEntityContext.class));
        order.verify(rowLevelSpecificationBuilder).build(any(), any());
        order.verify(specRepo).findOne(any(Specification.class));
        order.verify(repo).delete(any());
    }

    @Test
    void testUnknownEntityCodeThrows() {
        when(catalog.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        Pageable pageable = PageRequest.of(0, 10);
        SecuredLoadQuery query = SecuredLoadQuery.of("UNKNOWN", "base", pageable);

        assertThatThrownBy(() -> dataManager.loadByQuery(query)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testDeleteUnknownEntityCodeThrows() {
        when(catalog.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dataManager.delete("UNKNOWN", 1L)).isInstanceOf(IllegalArgumentException.class);
    }
}
