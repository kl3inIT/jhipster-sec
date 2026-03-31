package com.vn.core.security.serialize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.core.security.fetch.FetchPlan;
import com.vn.core.security.fetch.FetchPlanBuilder;
import com.vn.core.security.permission.AttributePermissionEvaluator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link SecureEntitySerializerImpl} verifying attribute filtering,
 * id always-visible rule (D-16), and denied attribute omission (D-15).
 * D-09: ObjectMapper replaces BeanWrapperImpl; these tests verify contract preservation.
 */
@ExtendWith(MockitoExtension.class)
class SecureEntitySerializerImplTest {

    @Mock
    private AttributePermissionEvaluator attributePermissionEvaluator;

    private SecureEntitySerializerImpl serializer;

    @BeforeEach
    void setUp() {
        serializer = new SecureEntitySerializerImpl(attributePermissionEvaluator, new ObjectMapper());
    }

    /** Test entity with getters (BeanWrapper requires them). */
    static class TestEntity {

        private Long id;
        private String name;
        private String secret;
        private String email;

        public TestEntity(Long id, String name, String secret, String email) {
            this.id = id;
            this.name = name;
            this.secret = secret;
            this.email = email;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getSecret() {
            return secret;
        }

        public String getEmail() {
            return email;
        }
    }

    static class ChildEntity {

        private String childName;

        public ChildEntity(String childName) {
            this.childName = childName;
        }

        public String getChildName() {
            return childName;
        }
    }

    static class ParentEntity {

        private Long id;
        private List<ChildEntity> children;

        public ParentEntity(Long id, List<ChildEntity> children) {
            this.id = id;
            this.children = children;
        }

        public Long getId() {
            return id;
        }

        public List<ChildEntity> getChildren() {
            return children;
        }
    }

    @Test
    void testIdAlwaysVisible() {
        // canView for "name" is called normally; canView for "id" is never called
        // because id is always-visible (D-16) and the implementation short-circuits
        when(attributePermissionEvaluator.canView(eq(TestEntity.class), eq("name"))).thenReturn(true);

        FetchPlan plan = new FetchPlanBuilder(TestEntity.class, "test").add("id").add("name").build();
        TestEntity entity = new TestEntity(42L, "Alice", "hidden", "a@b.com");

        Map<String, Object> result = serializer.serialize(entity, plan);

        // id is always visible per D-16 without requiring canView to return true
        assertThat(result).containsKey("id");
        assertThat(result.get("id")).isEqualTo(42L);
        assertThat(result).containsKey("name");
    }

    @Test
    void testDeniedAttributeOmitted() {
        when(attributePermissionEvaluator.canView(eq(TestEntity.class), eq("secret"))).thenReturn(false);

        FetchPlan plan = new FetchPlanBuilder(TestEntity.class, "test").add("secret").build();
        TestEntity entity = new TestEntity(1L, "Bob", "topsecret", "b@c.com");

        Map<String, Object> result = serializer.serialize(entity, plan);

        // denied attribute must be omitted per D-15
        assertThat(result).doesNotContainKey("secret");
    }

    @Test
    void testAllowedAttributeIncluded() {
        when(attributePermissionEvaluator.canView(eq(TestEntity.class), eq("name"))).thenReturn(true);

        FetchPlan plan = new FetchPlanBuilder(TestEntity.class, "test").add("name").build();
        TestEntity entity = new TestEntity(1L, "Charlie", "x", "c@d.com");

        Map<String, Object> result = serializer.serialize(entity, plan);

        assertThat(result).containsEntry("name", "Charlie");
    }

    @Test
    void testNullEntityReturnsNull() {
        FetchPlan plan = new FetchPlanBuilder(TestEntity.class, "test").add("id").build();

        Map<String, Object> result = serializer.serialize(null, plan);

        assertThat(result).isNull();
    }

    @Test
    void testCollectionReferenceSerializedRecursively() {
        when(attributePermissionEvaluator.canView(eq(ParentEntity.class), eq("children"))).thenReturn(true);
        when(attributePermissionEvaluator.canView(eq(ChildEntity.class), eq("childName"))).thenReturn(true);

        FetchPlan childPlan = new FetchPlanBuilder(ChildEntity.class, "child").add("childName").build();
        FetchPlan parentPlan = new FetchPlanBuilder(ParentEntity.class, "parent").add("id").add("children", childPlan).build();

        ParentEntity parent = new ParentEntity(10L, List.of(new ChildEntity("Alpha"), new ChildEntity("Beta")));

        Map<String, Object> result = serializer.serialize(parent, parentPlan);

        assertThat(result).containsKey("id");
        assertThat(result).containsKey("children");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> childResults = (List<Map<String, Object>>) result.get("children");
        assertThat(childResults).hasSize(2);
        assertThat(childResults.get(0)).containsEntry("childName", "Alpha");
        assertThat(childResults.get(1)).containsEntry("childName", "Beta");
    }

    @Test
    void testDeniedReferenceAttributeOmitted() {
        when(attributePermissionEvaluator.canView(eq(ParentEntity.class), eq("children"))).thenReturn(false);

        FetchPlan childPlan = new FetchPlanBuilder(ChildEntity.class, "child").add("childName").build();
        FetchPlan parentPlan = new FetchPlanBuilder(ParentEntity.class, "parent").add("id").add("children", childPlan).build();
        ParentEntity parent = new ParentEntity(10L, List.of(new ChildEntity("Alpha")));

        Map<String, Object> result = serializer.serialize(parent, parentPlan);

        assertThat(result).doesNotContainKey("children");
    }

    @Test
    void testMultiplePropertiesMixedPermissions() {
        when(attributePermissionEvaluator.canView(any(), eq("name"))).thenReturn(true);
        when(attributePermissionEvaluator.canView(any(), eq("email"))).thenReturn(false);
        when(attributePermissionEvaluator.canView(any(), eq("secret"))).thenReturn(false);

        FetchPlan plan = new FetchPlanBuilder(TestEntity.class, "test").add("id").add("name").add("email").add("secret").build();
        TestEntity entity = new TestEntity(5L, "Dave", "secretvalue", "d@e.com");

        Map<String, Object> result = serializer.serialize(entity, plan);

        assertThat(result).containsKey("id");
        assertThat(result).containsKey("name");
        assertThat(result).doesNotContainKey("email");
        assertThat(result).doesNotContainKey("secret");
    }
}
