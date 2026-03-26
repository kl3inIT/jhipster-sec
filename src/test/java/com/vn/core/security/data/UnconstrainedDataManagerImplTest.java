package com.vn.core.security.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vn.core.security.repository.RepositoryRegistry;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Unit tests for {@link UnconstrainedDataManagerImpl} verifying it bypasses all security
 * and delegates directly to repository. No security-related mocks exist per D-02.
 */
@ExtendWith(MockitoExtension.class)
class UnconstrainedDataManagerImplTest {

    @Mock
    private RepositoryRegistry repositoryRegistry;

    @InjectMocks
    private UnconstrainedDataManagerImpl dataManager;

    // Intentionally no mocks for AccessManager, RowLevelSpec, FetchPlanResolver, SecureEntitySerializer, etc.

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

    @Test
    @SuppressWarnings("unchecked")
    void testLoadDelegatesToRepository() {
        JpaRepository<TestEntity, Object> repo = mock(JpaRepository.class);
        when(repositoryRegistry.getRepository(TestEntity.class)).thenReturn((JpaRepository) repo);

        TestEntity entity = new TestEntity(5L);
        when(repo.findById(eq(5L))).thenReturn(Optional.of(entity));

        TestEntity result = dataManager.load(TestEntity.class, 5L);

        assertThat(result).isSameAs(entity);
        verify(repo).findById(5L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testLoadThrowsEntityNotFoundWhenMissing() {
        JpaRepository<TestEntity, Object> repo = mock(JpaRepository.class);
        when(repositoryRegistry.getRepository(TestEntity.class)).thenReturn((JpaRepository) repo);
        when(repo.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dataManager.load(TestEntity.class, 99L)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSaveDelegatesToRepository() {
        JpaRepository<TestEntity, Object> repo = mock(JpaRepository.class);
        when(repositoryRegistry.getRepository(TestEntity.class)).thenReturn((JpaRepository) repo);

        TestEntity entity = new TestEntity(1L);
        when(repo.save(entity)).thenReturn(entity);

        TestEntity saved = dataManager.save(entity);

        assertThat(saved).isSameAs(entity);
        verify(repo).save(entity);
    }

    @Test
    void testCreateReturnsNewInstance() {
        TestEntity first = dataManager.create(TestEntity.class);
        TestEntity second = dataManager.create(TestEntity.class);

        assertThat(first).isNotNull().isNotSameAs(second);
        assertThat(second).isNotNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testLoadOneDelegatesToSpecificationExecutor() {
        JpaSpecificationExecutor<TestEntity> specExecutor = mock(JpaSpecificationExecutor.class);
        when(repositoryRegistry.getSpecificationExecutor(TestEntity.class)).thenReturn((JpaSpecificationExecutor) specExecutor);

        Specification<TestEntity> spec = (root, query, cb) -> cb.conjunction();
        TestEntity entity = new TestEntity(7L);
        when(specExecutor.findOne(spec)).thenReturn(Optional.of(entity));

        Optional<TestEntity> result = dataManager.loadOne(TestEntity.class, spec);

        assertThat(result).containsSame(entity);
        verify(specExecutor).findOne(spec);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testLoadListDelegatesToSpecificationExecutor() {
        JpaSpecificationExecutor<TestEntity> specExecutor = mock(JpaSpecificationExecutor.class);
        when(repositoryRegistry.getSpecificationExecutor(TestEntity.class)).thenReturn((JpaSpecificationExecutor) specExecutor);

        Specification<TestEntity> spec = (root, query, cb) -> cb.conjunction();
        List<TestEntity> entities = List.of(new TestEntity(1L), new TestEntity(2L));
        when(specExecutor.findAll(spec)).thenReturn(entities);

        List<TestEntity> result = dataManager.loadList(TestEntity.class, spec);

        assertThat(result).containsExactlyElementsOf(entities);
        verify(specExecutor).findAll(spec);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testLoadPageDelegatesToSpecificationExecutor() {
        JpaSpecificationExecutor<TestEntity> specExecutor = mock(JpaSpecificationExecutor.class);
        when(repositoryRegistry.getSpecificationExecutor(TestEntity.class)).thenReturn((JpaSpecificationExecutor) specExecutor);

        Specification<TestEntity> spec = (root, query, cb) -> cb.conjunction();
        PageRequest pageable = PageRequest.of(0, 10);
        Page<TestEntity> page = new PageImpl<>(List.of(new TestEntity(1L)), pageable, 1);
        when(specExecutor.findAll(spec, pageable)).thenReturn(page);

        Page<TestEntity> result = dataManager.loadPage(TestEntity.class, spec, pageable);

        assertThat(result).isSameAs(page);
        verify(specExecutor).findAll(spec, pageable);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDeleteEntityDelegatesDirectlyToRepository() {
        JpaRepository<TestEntity, Object> repo = mock(JpaRepository.class);
        when(repositoryRegistry.getRepository(TestEntity.class)).thenReturn((JpaRepository) repo);

        TestEntity entity = new TestEntity(3L);

        dataManager.delete(entity);

        verify(repo).delete(entity);
        verify(repo, never()).findById(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDeleteLoadsAndDeletes() {
        JpaRepository<TestEntity, Object> repo = mock(JpaRepository.class);
        when(repositoryRegistry.getRepository(TestEntity.class)).thenReturn((JpaRepository) repo);

        TestEntity entity = new TestEntity(3L);
        when(repo.findById(eq(3L))).thenReturn(Optional.of(entity));

        dataManager.delete(TestEntity.class, 3L);

        verify(repo).findById(3L);
        verify(repo).delete(entity);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testLoadAllDelegatesToRepository() {
        JpaRepository<TestEntity, Object> repo = mock(JpaRepository.class);
        when(repositoryRegistry.getRepository(TestEntity.class)).thenReturn((JpaRepository) repo);

        List<TestEntity> entities = List.of(new TestEntity(1L), new TestEntity(2L));
        when(repo.findAll()).thenReturn(entities);

        List<TestEntity> result = dataManager.loadAll(TestEntity.class);

        assertThat(result).hasSize(2);
        verify(repo).findAll();
    }
}
