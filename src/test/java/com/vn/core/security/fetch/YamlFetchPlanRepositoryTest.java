package com.vn.core.security.fetch;

import static org.assertj.core.api.Assertions.assertThat;

import com.vn.core.config.ApplicationProperties;
import com.vn.core.domain.Department;
import com.vn.core.domain.Employee;
import com.vn.core.domain.Organization;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

/**
 * Unit tests for {@link YamlFetchPlanRepository} verifying YAML loading, extends inheritance,
 * and lookup behavior.
 */
class YamlFetchPlanRepositoryTest {

    // Test entity classes — must match the entity names used in fetch-plans-test.yml
    static class TestEntity {

        private Long id;
        private String name;
        private String email;

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

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    static class ParentEntity {

        private Long id;
        private String name;

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
    }

    private YamlFetchPlanRepository repository;

    @BeforeEach
    void setUp() {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        applicationProperties.getFetchPlans().setConfig("classpath:fetch-plans-test.yml");
        repository = new YamlFetchPlanRepository(applicationProperties, new DefaultResourceLoader());
        repository.init();
    }

    @Test
    void testLoadFromYaml() {
        Optional<FetchPlan> plan = repository.findByEntityAndName(TestEntity.class, "base");

        assertThat(plan).isPresent();
        assertThat(plan.get().getName()).isEqualTo("base");
        assertThat(plan.get().getProperties()).extracting(FetchPlanProperty::name).contains("id", "name");
    }

    @Test
    void testExtendsMergesProperties() {
        Optional<FetchPlan> plan = repository.findByEntityAndName(TestEntity.class, "detail");

        assertThat(plan).isPresent();
        // Should have parent properties (id, name) plus own (email)
        assertThat(plan.get().getProperties()).extracting(FetchPlanProperty::name).contains("id", "name", "email");
    }

    @Test
    void testMissingPlanReturnsEmpty() {
        Optional<FetchPlan> plan = repository.findByEntityAndName(TestEntity.class, "nonexistent");

        assertThat(plan).isEmpty();
    }

    @Test
    void testFindAllByEntity() {
        List<FetchPlan> plans = repository.findAllByEntity(TestEntity.class);

        // YAML defines: base, detail, with-child for TestEntity
        assertThat(plans).hasSizeGreaterThanOrEqualTo(2);
        assertThat(plans).extracting(FetchPlan::getName).contains("base", "detail");
    }

    @Test
    void testPlanForDifferentEntityIsIsolated() {
        Optional<FetchPlan> parentPlan = repository.findByEntityAndName(ParentEntity.class, "base");
        Optional<FetchPlan> entityPlan = repository.findByEntityAndName(TestEntity.class, "base");

        assertThat(parentPlan).isPresent();
        assertThat(entityPlan).isPresent();
        // Both exist as separate entries
        assertThat(parentPlan.get().getName()).isEqualTo("base");
        assertThat(entityPlan.get().getName()).isEqualTo("base");
    }

    @Test
    void testNestedInlinePropertiesAreParsedRecursively() {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        applicationProperties.getFetchPlans().setConfig("classpath:fetch-plans.yml");
        YamlFetchPlanRepository proofRepository = new YamlFetchPlanRepository(applicationProperties, new DefaultResourceLoader());
        proofRepository.init();

        Optional<FetchPlan> plan = proofRepository.findByEntityAndName(Organization.class, "organization-detail");

        assertThat(plan).isPresent();
        FetchPlan departmentsPlan = plan.get().getProperty("departments").orElseThrow().fetchPlan();
        assertThat(departmentsPlan).isNotNull();
        assertThat(departmentsPlan.getName()).isEqualTo("organization-detail:departments");
        assertThat(departmentsPlan.getProperties())
            .extracting(FetchPlanProperty::name)
            .contains("id", "code", "name", "costCenter", "employees");

        FetchPlan employeesPlan = departmentsPlan.getProperty("employees").orElseThrow().fetchPlan();
        assertThat(employeesPlan).isNotNull();
        assertThat(employeesPlan.getName()).isEqualTo("organization-detail:employees");
        assertThat(employeesPlan.getProperties())
            .extracting(FetchPlanProperty::name)
            .contains("id", "employeeNumber", "firstName", "lastName", "email", "salary");
    }

    @Test
    void testDepartmentAndEmployeeListPlansIncludeParentReferences() {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        applicationProperties.getFetchPlans().setConfig("classpath:fetch-plans.yml");
        YamlFetchPlanRepository proofRepository = new YamlFetchPlanRepository(applicationProperties, new DefaultResourceLoader());
        proofRepository.init();

        FetchPlan departmentList = proofRepository.findByEntityAndName(Department.class, "department-list").orElseThrow();
        FetchPlan organizationPlan = departmentList.getProperty("organization").orElseThrow().fetchPlan();
        assertThat(organizationPlan).isNotNull();
        assertThat(organizationPlan.getProperties()).extracting(FetchPlanProperty::name).containsExactly("id", "name");

        FetchPlan employeeList = proofRepository.findByEntityAndName(Employee.class, "employee-list").orElseThrow();
        FetchPlan departmentPlan = employeeList.getProperty("department").orElseThrow().fetchPlan();
        assertThat(departmentPlan).isNotNull();
        assertThat(departmentPlan.getProperties()).extracting(FetchPlanProperty::name).containsExactly("id", "name");
    }
}
