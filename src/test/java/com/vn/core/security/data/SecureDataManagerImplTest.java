package com.vn.core.security.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vn.core.security.catalog.SecuredEntityCatalog;
import com.vn.core.security.catalog.SecuredEntityEntry;
import com.vn.core.security.fetch.FetchPlan;
import com.vn.core.security.fetch.FetchPlanBuilder;
import com.vn.core.security.fetch.FetchPlanResolver;
import com.vn.core.security.merge.SecureMergeService;
import com.vn.core.security.permission.EntityOp;
import com.vn.core.security.row.RowLevelSpecificationBuilder;
import com.vn.core.security.serialize.SecureEntitySerializer;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.security.access.AccessDeniedException;

/**
 * Unit tests for {@link SecureDataManagerImpl} verifying the facade keeps row-policy,
 * fetch-plan, and serialization responsibilities while delegating secured mechanics
 * through {@link DataManager} and its explicit unconstrained bypass.
 */
@ExtendWith(MockitoExtension.class)
class SecureDataManagerImplTest {

    @Mock
    private DataManager dataManager;

    @Mock
    private UnconstrainedDataManager unconstrainedDataManager;

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
    private SecureQuerySpecificationFactory secureQuerySpecificationFactory;

    @InjectMocks
    private SecureDataManagerImpl secureDataManager;

    static class TestEntity {

        private Long id;

        public TestEntity() {}

        public TestEntity(Long id) {
            this.id = id;
        }

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
    void testLoadListDelegatesReadPageThroughDataManager() {
        when(catalog.findByCode("TEST_ENTITY")).thenReturn(Optional.of(testEntry));

        Specification<TestEntity> rowSpec = (root, query, cb) -> cb.conjunction();
        when(rowLevelSpecificationBuilder.build(TestEntity.class, EntityOp.READ)).thenReturn(rowSpec);

        TestEntity entity = new TestEntity(1L);
        Pageable pageable = PageRequest.of(0, 10);
        Page<TestEntity> page = new PageImpl<>(List.of(entity), pageable, 1);
        when(dataManager.loadPage(eq(TestEntity.class), any(Specification.class), eq(pageable), eq(EntityOp.READ))).thenReturn((Page) page);
        when(fetchPlanResolver.resolve(TestEntity.class, "base")).thenReturn(testFetchPlan);
        when(secureEntitySerializer.serialize(entity, testFetchPlan)).thenReturn(Map.of("id", 1L));

        Page<Map<String, Object>> result = secureDataManager.loadList("TEST_ENTITY", "base", pageable);

        assertThat(result.getContent()).containsExactly(Map.of("id", 1L));

        InOrder order = inOrder(rowLevelSpecificationBuilder, dataManager, fetchPlanResolver, secureEntitySerializer);
        order.verify(rowLevelSpecificationBuilder).build(TestEntity.class, EntityOp.READ);
        order.verify(dataManager).loadPage(eq(TestEntity.class), any(Specification.class), eq(pageable), eq(EntityOp.READ));
        order.verify(fetchPlanResolver).resolve(TestEntity.class, "base");
        order.verify(secureEntitySerializer).serialize(entity, testFetchPlan);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testLoadByQueryEnforcesReadOrder() {
        when(catalog.findByCode("TEST_ENTITY")).thenReturn(Optional.of(testEntry));

        Specification<TestEntity> rowSpec = (root, query, cb) -> cb.conjunction();
        when(rowLevelSpecificationBuilder.build(TestEntity.class, EntityOp.READ)).thenReturn(rowSpec);
        when(secureQuerySpecificationFactory.build(TestEntity.class, Map.of())).thenReturn((root, query, cb) -> null);

        TestEntity entity = new TestEntity(1L);
        Pageable pageable = PageRequest.of(0, 10);
        Page<TestEntity> page = new PageImpl<>(List.of(entity), pageable, 1);
        when(dataManager.loadPage(eq(TestEntity.class), any(Specification.class), eq(pageable), eq(EntityOp.READ))).thenReturn((Page) page);
        when(fetchPlanResolver.resolve(TestEntity.class, "base")).thenReturn(testFetchPlan);
        when(secureEntitySerializer.serialize(entity, testFetchPlan)).thenReturn(Map.of("id", 1L));

        Page<Map<String, Object>> result = secureDataManager.loadByQuery(SecuredLoadQuery.of("TEST_ENTITY", "base", pageable));

        assertThat(result.getContent()).containsExactly(Map.of("id", 1L));

        InOrder order = inOrder(rowLevelSpecificationBuilder, dataManager, fetchPlanResolver, secureEntitySerializer);
        order.verify(rowLevelSpecificationBuilder).build(TestEntity.class, EntityOp.READ);
        verify(secureQuerySpecificationFactory).build(TestEntity.class, Map.of());
        order.verify(dataManager).loadPage(eq(TestEntity.class), any(Specification.class), eq(pageable), eq(EntityOp.READ));
        order.verify(fetchPlanResolver).resolve(TestEntity.class, "base");
        order.verify(secureEntitySerializer).serialize(entity, testFetchPlan);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testLoadByQueryBlankJpqlDelegatesThroughDataManagerLoadPage() {
        when(catalog.findByCode("TEST_ENTITY")).thenReturn(Optional.of(testEntry));

        Specification<TestEntity> rowSpec = (root, query, cb) -> cb.conjunction();
        when(rowLevelSpecificationBuilder.build(TestEntity.class, EntityOp.READ)).thenReturn(rowSpec);

        Map<String, Object> filters = Map.of("ownerLogin", "proof-owner");
        Specification<TestEntity> filterSpec = (root, query, cb) -> cb.equal(root.get("ownerLogin"), "proof-owner");
        when(secureQuerySpecificationFactory.build(TestEntity.class, filters)).thenReturn(filterSpec);

        TestEntity entity = new TestEntity(1L);
        Pageable pageable = PageRequest.of(0, 10);
        Page<TestEntity> page = new PageImpl<>(List.of(entity), pageable, 1);
        when(dataManager.loadPage(eq(TestEntity.class), any(Specification.class), eq(pageable), eq(EntityOp.READ))).thenReturn((Page) page);
        when(fetchPlanResolver.resolve(TestEntity.class, "base")).thenReturn(testFetchPlan);
        when(secureEntitySerializer.serialize(entity, testFetchPlan)).thenReturn(Map.of("id", 1L));

        SecuredLoadQuery query = new SecuredLoadQuery("TEST_ENTITY", "   ", filters, pageable, pageable.getSort(), "base");

        Page<Map<String, Object>> result = secureDataManager.loadByQuery(query);

        assertThat(result.getContent()).containsExactly(Map.of("id", 1L));
        verify(secureQuerySpecificationFactory).build(TestEntity.class, filters);
        verify(dataManager).loadPage(eq(TestEntity.class), any(Specification.class), eq(pageable), eq(EntityOp.READ));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testLoadByQueryCombinesRowLevelAndFilterSpecifications() {
        when(catalog.findByCode("TEST_ENTITY")).thenReturn(Optional.of(testEntry));

        AtomicBoolean rowInvoked = new AtomicBoolean(false);
        Predicate rowPredicate = mock(Predicate.class);
        Specification<TestEntity> rowSpec = (root, query, cb) -> {
            rowInvoked.set(true);
            return rowPredicate;
        };
        when(rowLevelSpecificationBuilder.build(TestEntity.class, EntityOp.READ)).thenReturn(rowSpec);

        Map<String, Object> filters = Map.of("ownerLogin", "proof-owner");
        AtomicBoolean filterInvoked = new AtomicBoolean(false);
        Predicate filterPredicate = mock(Predicate.class);
        Specification<TestEntity> filterSpec = (root, query, cb) -> {
            filterInvoked.set(true);
            return filterPredicate;
        };
        when(secureQuerySpecificationFactory.build(TestEntity.class, filters)).thenReturn(filterSpec);

        TestEntity entity = new TestEntity(1L);
        Pageable pageable = PageRequest.of(0, 10);
        Page<TestEntity> page = new PageImpl<>(List.of(entity), pageable, 1);
        when(dataManager.loadPage(eq(TestEntity.class), any(Specification.class), eq(pageable), eq(EntityOp.READ))).thenReturn((Page) page);
        when(fetchPlanResolver.resolve(TestEntity.class, "base")).thenReturn(testFetchPlan);
        when(secureEntitySerializer.serialize(entity, testFetchPlan)).thenReturn(Map.of("id", 1L));

        secureDataManager.loadByQuery(new SecuredLoadQuery("TEST_ENTITY", null, filters, pageable, pageable.getSort(), "base"));

        ArgumentCaptor<Specification<TestEntity>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(dataManager).loadPage(eq(TestEntity.class), specCaptor.capture(), eq(pageable), eq(EntityOp.READ));

        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        CriteriaQuery<?> criteriaQuery = mock(CriteriaQuery.class);
        Root<TestEntity> root = mock(Root.class);
        Predicate combinedPredicate = mock(Predicate.class);
        when(criteriaBuilder.and(rowPredicate, filterPredicate)).thenReturn(combinedPredicate);

        assertThat(specCaptor.getValue().toPredicate(root, criteriaQuery, criteriaBuilder)).isSameAs(combinedPredicate);
        assertThat(rowInvoked).isTrue();
        assertThat(filterInvoked).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testLoadOneReturnsSerializedEntity() {
        when(catalog.findByCode("TEST_ENTITY")).thenReturn(Optional.of(testEntry));

        Specification<TestEntity> rowSpec = (root, query, cb) -> cb.conjunction();
        when(rowLevelSpecificationBuilder.build(TestEntity.class, EntityOp.READ)).thenReturn(rowSpec);

        TestEntity entity = new TestEntity(1L);
        when(dataManager.loadOne(eq(TestEntity.class), any(Specification.class), eq(EntityOp.READ))).thenReturn(Optional.of(entity));
        when(fetchPlanResolver.resolve(TestEntity.class, "base")).thenReturn(testFetchPlan);
        when(secureEntitySerializer.serialize(entity, testFetchPlan)).thenReturn(Map.of("id", 1L));

        Optional<Map<String, Object>> result = secureDataManager.loadOne("TEST_ENTITY", 1L, "base");

        assertThat(result).contains(Map.of("id", 1L));

        InOrder order = inOrder(rowLevelSpecificationBuilder, dataManager, fetchPlanResolver, secureEntitySerializer);
        order.verify(rowLevelSpecificationBuilder).build(TestEntity.class, EntityOp.READ);
        order.verify(dataManager).loadOne(eq(TestEntity.class), any(Specification.class), eq(EntityOp.READ));
        order.verify(fetchPlanResolver).resolve(TestEntity.class, "base");
        order.verify(secureEntitySerializer).serialize(entity, testFetchPlan);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testLoadOneReturnsEmptyWhenRowIsFilteredOut() {
        when(catalog.findByCode("TEST_ENTITY")).thenReturn(Optional.of(testEntry));

        Specification<TestEntity> rowSpec = (root, query, cb) -> cb.conjunction();
        when(rowLevelSpecificationBuilder.build(TestEntity.class, EntityOp.READ)).thenReturn(rowSpec);
        when(dataManager.loadOne(eq(TestEntity.class), any(Specification.class), eq(EntityOp.READ))).thenReturn(Optional.empty());

        assertThat(secureDataManager.loadOne("TEST_ENTITY", 99L, "base")).isEmpty();
        verify(fetchPlanResolver, never()).resolve(any(), any());
        verify(secureEntitySerializer, never()).serialize(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testLoadByQueryUnsupportedJpqlFailsClosed() {
        SecuredEntityEntry jpqlEntry = SecuredEntityEntry.builder()
            .entityClass(TestEntity.class)
            .code("TEST_ENTITY")
            .operations(EnumSet.allOf(EntityOp.class))
            .fetchPlanCodes(List.of("base"))
            .jpqlAllowed(true)
            .build();
        when(catalog.findByCode("TEST_ENTITY")).thenReturn(Optional.of(jpqlEntry));

        Specification<TestEntity> rowSpec = (root, query, cb) -> cb.conjunction();
        when(rowLevelSpecificationBuilder.build(TestEntity.class, EntityOp.READ)).thenReturn(rowSpec);

        SecuredLoadQuery query = new SecuredLoadQuery(
            "TEST_ENTITY",
            "select e from TestEntity e",
            Map.of(),
            PageRequest.of(0, 10),
            PageRequest.of(0, 10).getSort(),
            "base"
        );

        assertThatThrownBy(() -> secureDataManager.loadByQuery(query))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("JPQL query translation is not implemented for secured queries: select e from TestEntity e");
    }

    @Test
    void testLoadByQueryDeniedCrudThrows() {
        when(catalog.findByCode("TEST_ENTITY")).thenReturn(Optional.of(testEntry));
        when(rowLevelSpecificationBuilder.build(TestEntity.class, EntityOp.READ)).thenReturn((root, query, cb) -> cb.conjunction());
        when(secureQuerySpecificationFactory.build(TestEntity.class, Map.of())).thenReturn((root, query, cb) -> null);
        when(dataManager.loadPage(eq(TestEntity.class), any(Specification.class), any(Pageable.class), eq(EntityOp.READ))).thenThrow(
            new AccessDeniedException("READ denied on TestEntity")
        );

        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> secureDataManager.loadByQuery(SecuredLoadQuery.of("TEST_ENTITY", "base", pageable))).isInstanceOf(
            AccessDeniedException.class
        );
    }

    @Test
    void testSaveCreateUsesUnconstrainedCreateAndSave() {
        when(catalog.findByCode("TEST_ENTITY")).thenReturn(Optional.of(testEntry));
        when(dataManager.unconstrained()).thenReturn(unconstrainedDataManager);

        TestEntity entity = new TestEntity();
        TestEntity saved = new TestEntity(1L);
        when(unconstrainedDataManager.create(TestEntity.class)).thenReturn(entity);
        when(unconstrainedDataManager.save(entity)).thenReturn(saved);
        when(fetchPlanResolver.resolve(TestEntity.class, "base")).thenReturn(testFetchPlan);
        when(secureEntitySerializer.serialize(saved, testFetchPlan)).thenReturn(Map.of("id", 1L));

        Map<String, Object> attributes = Map.of("name", "test");
        Map<String, Object> result = secureDataManager.save("TEST_ENTITY", null, attributes, "base");

        assertThat(result).isEqualTo(Map.of("id", 1L));

        InOrder order = inOrder(dataManager, unconstrainedDataManager, secureMergeService, fetchPlanResolver, secureEntitySerializer);
        order.verify(dataManager).checkCrud(TestEntity.class, EntityOp.CREATE);
        order.verify(dataManager).unconstrained();
        order.verify(unconstrainedDataManager).create(TestEntity.class);
        order.verify(secureMergeService).mergeForUpdate(entity, attributes);
        order.verify(unconstrainedDataManager).save(entity);
        order.verify(fetchPlanResolver).resolve(TestEntity.class, "base");
        order.verify(secureEntitySerializer).serialize(saved, testFetchPlan);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSaveUpdateUsesDataManagerLookupAndUnconstrainedSave() {
        when(catalog.findByCode("TEST_ENTITY")).thenReturn(Optional.of(testEntry));
        when(dataManager.unconstrained()).thenReturn(unconstrainedDataManager);

        Specification<TestEntity> rowSpec = (root, query, cb) -> cb.conjunction();
        when(rowLevelSpecificationBuilder.build(TestEntity.class, EntityOp.UPDATE)).thenReturn(rowSpec);

        TestEntity entity = new TestEntity(1L);
        TestEntity saved = new TestEntity(1L);
        when(dataManager.loadOne(eq(TestEntity.class), any(Specification.class), eq(EntityOp.UPDATE))).thenReturn(Optional.of(entity));
        when(unconstrainedDataManager.save(entity)).thenReturn(saved);
        when(fetchPlanResolver.resolve(TestEntity.class, "base")).thenReturn(testFetchPlan);
        when(secureEntitySerializer.serialize(saved, testFetchPlan)).thenReturn(Map.of("id", 1L));

        Map<String, Object> attributes = Map.of("name", "updated");
        Map<String, Object> result = secureDataManager.save("TEST_ENTITY", 1L, attributes, "base");

        assertThat(result).isEqualTo(Map.of("id", 1L));

        InOrder order = inOrder(rowLevelSpecificationBuilder, dataManager, secureMergeService, unconstrainedDataManager, fetchPlanResolver, secureEntitySerializer);
        order.verify(rowLevelSpecificationBuilder).build(TestEntity.class, EntityOp.UPDATE);
        order.verify(dataManager).loadOne(eq(TestEntity.class), any(Specification.class), eq(EntityOp.UPDATE));
        order.verify(dataManager).unconstrained();
        order.verify(secureMergeService).mergeForUpdate(entity, attributes);
        order.verify(unconstrainedDataManager).save(entity);
        order.verify(fetchPlanResolver).resolve(TestEntity.class, "base");
        order.verify(secureEntitySerializer).serialize(saved, testFetchPlan);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDeleteEnforcesDeleteOrder() {
        when(catalog.findByCode("TEST_ENTITY")).thenReturn(Optional.of(testEntry));
        when(dataManager.unconstrained()).thenReturn(unconstrainedDataManager);

        Specification<TestEntity> rowSpec = (root, query, cb) -> cb.conjunction();
        when(rowLevelSpecificationBuilder.build(TestEntity.class, EntityOp.DELETE)).thenReturn(rowSpec);

        TestEntity entity = new TestEntity(1L);
        when(dataManager.loadOne(eq(TestEntity.class), any(Specification.class), eq(EntityOp.DELETE))).thenReturn(Optional.of(entity));

        secureDataManager.delete("TEST_ENTITY", 1L);

        InOrder order = inOrder(rowLevelSpecificationBuilder, dataManager, unconstrainedDataManager);
        order.verify(rowLevelSpecificationBuilder).build(TestEntity.class, EntityOp.DELETE);
        order.verify(dataManager).loadOne(eq(TestEntity.class), any(Specification.class), eq(EntityOp.DELETE));
        order.verify(dataManager).unconstrained();
        order.verify(unconstrainedDataManager).delete(entity);
    }

    @Test
    void testUnknownEntityCodeThrows() {
        when(catalog.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> secureDataManager.loadByQuery(SecuredLoadQuery.of("UNKNOWN", "base", pageable))).isInstanceOf(
            IllegalArgumentException.class
        );
    }

    @Test
    void testDeleteUnknownEntityCodeThrows() {
        when(catalog.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> secureDataManager.delete("UNKNOWN", 1L)).isInstanceOf(IllegalArgumentException.class);
    }
}
