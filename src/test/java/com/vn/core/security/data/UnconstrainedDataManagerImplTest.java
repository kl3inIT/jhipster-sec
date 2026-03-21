package com.vn.core.security.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import org.springframework.data.jpa.repository.JpaRepository;

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
