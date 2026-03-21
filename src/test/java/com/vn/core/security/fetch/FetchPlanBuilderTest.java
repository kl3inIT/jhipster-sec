package com.vn.core.security.fetch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link FetchPlanBuilder} verifying code-based plan construction.
 */
class FetchPlanBuilderTest {

    static class TestEntity {

        private Long id;
        private String name;
    }

    static class ChildEntity {

        private Long id;
    }

    @Test
    void testBuildSimplePlan() {
        FetchPlan plan = new FetchPlanBuilder(TestEntity.class, "simple").add("id").add("name").build();

        assertThat(plan.getName()).isEqualTo("simple");
        assertThat(plan.getEntityClass()).isEqualTo(TestEntity.class);
        assertThat(plan.getProperties()).hasSize(2);
        assertThat(plan.getProperties()).extracting(FetchPlanProperty::name).containsExactly("id", "name");
    }

    @Test
    void testBuildWithNestedPlan() {
        FetchPlan childPlan = new FetchPlanBuilder(ChildEntity.class, "child-base").add("id").build();

        FetchPlan plan = new FetchPlanBuilder(TestEntity.class, "with-child").add("id").add("child", childPlan).build();

        assertThat(plan.getProperties()).hasSize(2);
        assertThat(plan.getProperty("child")).isPresent();
        assertThat(plan.getProperty("child").get().fetchPlan()).isNotNull();
        assertThat(plan.getProperty("child").get().fetchPlan().getName()).isEqualTo("child-base");
    }

    @Test
    void testBuildProducesImmutableProperties() {
        FetchPlan plan = new FetchPlanBuilder(TestEntity.class, "immutable").add("id").build();

        List<FetchPlanProperty> properties = plan.getProperties();
        assertThatThrownBy(() -> properties.add(new FetchPlanProperty("extra"))).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testBuildWithNamedNestedReference() {
        FetchPlan plan = new FetchPlanBuilder(TestEntity.class, "ref").add("id").add("child", "child-minimal").build();

        assertThat(plan.getProperty("child")).isPresent();
        assertThat(plan.getProperty("child").get().fetchPlan()).isNotNull();
        assertThat(plan.getProperty("child").get().fetchPlan().getName()).isEqualTo("child-minimal");
    }

    @Test
    void testEmptyPlanHasNoProperties() {
        FetchPlan plan = new FetchPlanBuilder(TestEntity.class, "empty").build();

        assertThat(plan.getProperties()).isEmpty();
    }
}
