package com.vn.core.security.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vn.core.security.catalog.SecuredEntityCatalog;
import com.vn.core.security.catalog.SecuredEntityEntry;
import com.vn.core.security.fetch.FetchPlan;
import com.vn.core.security.fetch.FetchPlanBuilder;
import com.vn.core.security.fetch.FetchPlanResolver;
import com.vn.core.security.merge.SecureMergeService;
import com.vn.core.security.permission.EntityOp;
import com.vn.core.security.serialize.SecureEntitySerializer;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

/**
 * Unit tests for {@link SecureDataManagerImpl} in the post-row-policy secured-data flow.
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
    void loadList_serializesEntitiesFromLegacyWrapper() {
        when(catalog.findByCode("TEST_ENTITY")).thenReturn(Optional.of(testEntry));

        TestEntity entity = new TestEntity(1L);
        Pageable pageable = PageRequest.of(0, 10);
        when(dataManager.loadPage(TestEntity.class, null, pageable, EntityOp.READ)).thenReturn(
            new PageImpl<>(List.of(entity), pageable, 1)
        );
        when(fetchPlanResolver.resolve(TestEntity.class, "base")).thenReturn(testFetchPlan);
        when(secureEntitySerializer.serialize(entity, testFetchPlan)).thenReturn(Map.of("id", 1L));

        Page<Map<String, Object>> result = secureDataManager.loadList("TEST_ENTITY", "base", pageable);

        assertThat(result.getContent()).containsExactly(Map.of("id", 1L));
        verify(dataManager).loadPage(TestEntity.class, null, pageable, EntityOp.READ);
    }

    @Test
    void loadByQuery_withFiltersDelegatesReadPageAndSerializes() {
        when(catalog.findByCode("TEST_ENTITY")).thenReturn(Optional.of(testEntry));

        Map<String, Object> filters = Map.of("id", 1L);
        Specification<TestEntity> filterSpec = (root, query, cb) -> cb.equal(root.get("id"), 1L);
        when(secureQuerySpecificationFactory.build(TestEntity.class, filters)).thenReturn(filterSpec);

        TestEntity entity = new TestEntity(1L);
        Pageable pageable = PageRequest.of(0, 10);
        when(dataManager.loadPage(eq(TestEntity.class), eq(filterSpec), eq(pageable), eq(EntityOp.READ))).thenReturn(
            new PageImpl<>(List.of(entity), pageable, 1)
        );
        when(fetchPlanResolver.resolve(TestEntity.class, "base")).thenReturn(testFetchPlan);
        when(secureEntitySerializer.serialize(entity, testFetchPlan)).thenReturn(Map.of("id", 1L));

        Page<Map<String, Object>> result = secureDataManager.loadByQuery(
            new SecuredLoadQuery("TEST_ENTITY", null, filters, pageable, pageable.getSort(), "base")
        );

        assertThat(result.getContent()).containsExactly(Map.of("id", 1L));
        verify(secureQuerySpecificationFactory).build(TestEntity.class, filters);
    }

    @Test
    void loadByQuery_withUnsupportedJpqlFailsClosed() {
        SecuredEntityEntry jpqlEntry = SecuredEntityEntry.builder()
            .entityClass(TestEntity.class)
            .code("TEST_ENTITY")
            .operations(EnumSet.allOf(EntityOp.class))
            .fetchPlanCodes(List.of("base"))
            .jpqlAllowed(true)
            .build();
        when(catalog.findByCode("TEST_ENTITY")).thenReturn(Optional.of(jpqlEntry));

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
    void loadOne_returnsSerializedEntityFromLegacyWrapper() {
        when(catalog.findByCode("TEST_ENTITY")).thenReturn(Optional.of(testEntry));

        TestEntity entity = new TestEntity(1L);
        when(dataManager.loadOne(eq(TestEntity.class), any(Specification.class), eq(EntityOp.READ))).thenReturn(Optional.of(entity));
        when(fetchPlanResolver.resolve(TestEntity.class, "base")).thenReturn(testFetchPlan);
        when(secureEntitySerializer.serialize(entity, testFetchPlan)).thenReturn(Map.of("id", 1L));

        Optional<Map<String, Object>> result = secureDataManager.loadOne("TEST_ENTITY", 1L, "base");

        assertThat(result).contains(Map.of("id", 1L));
    }

    @Test
    void delete_loadsByIdAndDeletesThroughUnconstrainedManager() {
        when(catalog.findByCode("TEST_ENTITY")).thenReturn(Optional.of(testEntry));
        when(dataManager.unconstrained()).thenReturn(unconstrainedDataManager);

        TestEntity managedEntity = new TestEntity(1L);
        when(dataManager.loadOne(eq(TestEntity.class), any(Specification.class), eq(EntityOp.DELETE))).thenReturn(
            Optional.of(managedEntity)
        );

        secureDataManager.delete("TEST_ENTITY", 1L);

        verify(unconstrainedDataManager).delete(managedEntity);
    }

    @Test
    void unknownEntityCodeThrows() {
        when(catalog.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> secureDataManager.delete("UNKNOWN", 1L)).isInstanceOf(IllegalArgumentException.class);
        verify(fetchPlanResolver, never()).resolve(any(), any());
    }
}
