package com.vn.core.security.merge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vn.core.security.permission.AttributePermissionEvaluator;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/**
 * Unit tests for {@link SecureMergeServiceImpl} verifying write-side attribute enforcement per D-18.
 */
@ExtendWith(MockitoExtension.class)
class SecureMergeServiceImplTest {

    @Mock
    private AttributePermissionEvaluator attributePermissionEvaluator;

    @InjectMocks
    private SecureMergeServiceImpl mergeService;

    /** Test entity with mutable fields (BeanWrapper requires setters). */
    static class TestEntity {

        private Long id = 1L;
        private String name;
        private String secret;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }

    @Test
    void testPermittedAttributeWritten() {
        when(attributePermissionEvaluator.canEdit(eq(TestEntity.class), eq("name"))).thenReturn(true);

        TestEntity entity = new TestEntity();
        Map<String, Object> attributes = Map.of("name", "NewName");
        mergeService.mergeForUpdate(entity, attributes);

        assertThat(entity.getName()).isEqualTo("NewName");
    }

    @Test
    void testDeniedAttributeThrowsAccessDenied() {
        when(attributePermissionEvaluator.canEdit(eq(TestEntity.class), eq("secret"))).thenReturn(false);

        TestEntity entity = new TestEntity();
        Map<String, Object> attributes = Map.of("secret", "hacked");

        assertThatThrownBy(() -> mergeService.mergeForUpdate(entity, attributes))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("secret");
    }

    @Test
    void testIdAttributeSkipped() {
        TestEntity entity = new TestEntity();
        Long originalId = entity.getId();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 999L);

        // Should not throw — id is silently skipped
        mergeService.mergeForUpdate(entity, attributes);

        // id should not have been changed
        assertThat(entity.getId()).isEqualTo(originalId);
        // canEdit should never be called for id
        verify(attributePermissionEvaluator, never()).canEdit(any(), eq("id"));
    }

    @Test
    void testMixedAttributesWritesPermittedSkipsDenied() {
        when(attributePermissionEvaluator.canEdit(eq(TestEntity.class), eq("name"))).thenReturn(true);
        when(attributePermissionEvaluator.canEdit(eq(TestEntity.class), eq("secret"))).thenReturn(false);

        TestEntity entity = new TestEntity();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("name", "Alice");
        attributes.put("secret", "forbidden");

        // Should throw because secret is denied
        assertThatThrownBy(() -> mergeService.mergeForUpdate(entity, attributes)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testEmptyAttributesMapDoesNothing() {
        TestEntity entity = new TestEntity();

        // Should not throw with empty map
        mergeService.mergeForUpdate(entity, Map.of());

        verify(attributePermissionEvaluator, never()).canEdit(any(), any());
    }
}
