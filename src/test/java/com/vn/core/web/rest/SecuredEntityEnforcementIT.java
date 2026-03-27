package com.vn.core.web.rest;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.core.IntegrationTest;
import com.vn.core.security.AuthoritiesConstants;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests proving allow and deny behavior for the secured entities.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class SecuredEntityEnforcementIT {

    private static final String ORGANIZATION_API_URL = "/api/organizations";
    private static final String DEPARTMENT_API_URL = "/api/departments";
    private static final String EMPLOYEE_API_URL = "/api/employees";
    private static final String PERMISSION_ADMIN_API_URL = "/api/admin/sec/permissions";

    @Autowired
    private MockMvc restMockMvc;

    @Autowired
    private ObjectMapper om;

    @Test
    @WithMockUser(username = "proof-owner", authorities = "ROLE_PROOF_READER")
    void getOrganizations_returnsOwnedRowsOnly() throws Exception {
        grantReadableOrganizationGraph("ROLE_PROOF_READER");

        restMockMvc
            .perform(get(ORGANIZATION_API_URL + "?sort=id,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(100))
            .andExpect(jsonPath("$[0].code").value("ORG-OWNED"))
            .andExpect(jsonPath("$[0].budget").doesNotExist());
    }

    @Test
    @WithMockUser(username = "proof-owner", authorities = "ROLE_PROOF_READER")
    void getOrganization_returnsNestedDataAndOmitsDeniedFields() throws Exception {
        grantReadableOrganizationGraph("ROLE_PROOF_READER");

        restMockMvc
            .perform(get(ORGANIZATION_API_URL + "/{id}", 100))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(100))
            .andExpect(jsonPath("$.code").value("ORG-OWNED"))
            .andExpect(jsonPath("$.name").value("Owned Org"))
            .andExpect(jsonPath("$.budget").doesNotExist())
            .andExpect(jsonPath("$.departments[0].id").value(200))
            .andExpect(jsonPath("$.departments[0].name").value("Owned Department"))
            .andExpect(jsonPath("$.departments[0].employees[0].id").value(300))
            .andExpect(jsonPath("$.departments[0].employees[0].firstName").value("Alice"))
            .andExpect(jsonPath("$.departments[0].employees[0].salary").doesNotExist());
    }

    @Test
    @WithMockUser(username = "proof-owner", authorities = "ROLE_PROOF_NONE")
    void getOrganizations_withoutReadPermission_returnsForbidden() throws Exception {
        restMockMvc.perform(get(ORGANIZATION_API_URL)).andExpect(status().isForbidden());
    }

    @Test
    void getOrganizations_matrixCreatedPermissionChangesForbiddenToOk() throws Exception {
        Map<String, Object> permissionPayload = Map.of(
            "authorityName",
            "ROLE_PROOF_NONE",
            "targetType",
            "ENTITY",
            "target",
            "organization",
            "action",
            "READ",
            "effect",
            "GRANT"
        );

        restMockMvc
            .perform(get(ORGANIZATION_API_URL + "?sort=id,asc").with(user("proof-owner").authorities(new SimpleGrantedAuthority("ROLE_PROOF_NONE"))))
            .andExpect(status().isForbidden());

        restMockMvc
            .perform(
                post(PERMISSION_ADMIN_API_URL)
                    .with(user("admin").authorities(new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(permissionPayload))
            )
            .andExpect(status().isCreated());

        grantReadableOrganizationGraph("ROLE_PROOF_NONE");

        restMockMvc
            .perform(get(ORGANIZATION_API_URL + "?sort=id,asc").with(user("proof-owner").authorities(new SimpleGrantedAuthority("ROLE_PROOF_NONE"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].code").value("ORG-OWNED"));
    }

    @Test
    void getOrganizations_unionOfAllowAcrossAuthorities_returnsOwnedRow() throws Exception {
        Map<String, Object> denyPermissionPayload = Map.of(
            "authorityName",
            "ROLE_PROOF_NONE",
            "targetType",
            "ENTITY",
            "target",
            "organization",
            "action",
            "READ",
            "effect",
            "DENY"
        );

        restMockMvc
            .perform(
                post(PERMISSION_ADMIN_API_URL)
                    .with(user("admin").authorities(new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(denyPermissionPayload))
            )
            .andExpect(status().isCreated());

        grantReadableOrganizationGraph("ROLE_PROOF_READER");

        restMockMvc
            .perform(
                get(ORGANIZATION_API_URL + "?sort=id,asc")
                    .with(
                        user("proof-owner")
                            .authorities(new SimpleGrantedAuthority("ROLE_PROOF_READER"), new SimpleGrantedAuthority("ROLE_PROOF_NONE"))
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(100))
            .andExpect(jsonPath("$[0].code").value("ORG-OWNED"));
    }

    @Test
    @WithMockUser(username = "proof-owner", authorities = "ROLE_PROOF_READER")
    void getOrganization_outsideRowPolicy_returnsNotFound() throws Exception {
        restMockMvc.perform(get(ORGANIZATION_API_URL + "/{id}", 101)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "proof-owner", authorities = "ROLE_PROOF_EDITOR")
    void createOrganization_withEditorRole_returnsCreated() throws Exception {
        grantReadableOrganizationGraph("ROLE_PROOF_EDITOR");
        grantEditableOrganizationFields("ROLE_PROOF_EDITOR");

        Map<String, Object> payload = Map.of("code", "ORG-NEW", "name", "New Org", "ownerLogin", "proof-owner");

        restMockMvc
            .perform(post(ORGANIZATION_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(payload)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("ORG-NEW"))
            .andExpect(jsonPath("$.name").value("New Org"));

        restMockMvc
            .perform(get(ORGANIZATION_API_URL + "?sort=id,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$.[*].code").value(hasItem("ORG-NEW")));
    }

    @Test
    @WithMockUser(username = "proof-owner", authorities = "ROLE_PROOF_EDITOR")
    void updateOrganization_withAllowedFields_returnsUpdatedPayload() throws Exception {
        grantReadableOrganizationGraph("ROLE_PROOF_EDITOR");
        grantEditableOrganizationFields("ROLE_PROOF_EDITOR");

        Map<String, Object> payload = Map.of("name", "Owned Org Updated");

        restMockMvc
            .perform(put(ORGANIZATION_API_URL + "/{id}", 100).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(100))
            .andExpect(jsonPath("$.code").value("ORG-OWNED"))
            .andExpect(jsonPath("$.name").value("Owned Org Updated"));

        restMockMvc
            .perform(get(ORGANIZATION_API_URL + "/{id}", 100))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(100))
            .andExpect(jsonPath("$.name").value("Owned Org Updated"));
    }

    @Test
    @WithMockUser(username = "proof-owner", authorities = "ROLE_PROOF_EDITOR")
    void updateOrganization_withDeniedBudgetEdit_returnsForbidden() throws Exception {
        grantReadableOrganizationGraph("ROLE_PROOF_EDITOR");
        grantEditableOrganizationFields("ROLE_PROOF_EDITOR");

        Map<String, Object> payload = Map.of("name", "Owned Org Updated", "budget", 999999.00);

        restMockMvc
            .perform(put(ORGANIZATION_API_URL + "/{id}", 100).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(payload)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "proof-owner", authorities = "ROLE_PROOF_EDITOR")
    void deleteOrganization_outsideRowPolicy_returnsForbidden() throws Exception {
        restMockMvc.perform(delete(ORGANIZATION_API_URL + "/{id}", 101)).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "proof-owner", authorities = "ROLE_PROOF_READER")
    void getDepartments_withReadPermission_returnsOk() throws Exception {
        restMockMvc
            .perform(get(DEPARTMENT_API_URL + "?sort=id,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[*].id").value(hasItem(200)));
    }

    @Test
    @WithMockUser(username = "proof-owner", authorities = "ROLE_PROOF_READER")
    void getEmployees_withReadPermission_returnsOk() throws Exception {
        restMockMvc
            .perform(get(EMPLOYEE_API_URL + "?sort=id,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[*].id").value(hasItem(300)));
    }

    private void grantReadableOrganizationGraph(String authorityName) throws Exception {
        grantAttributePermission(authorityName, "organization.code", "VIEW");
        grantAttributePermission(authorityName, "organization.name", "VIEW");
        grantAttributePermission(authorityName, "organization.ownerLogin", "VIEW");
        grantAttributePermission(authorityName, "organization.departments", "VIEW");
        grantAttributePermission(authorityName, "department.code", "VIEW");
        grantAttributePermission(authorityName, "department.name", "VIEW");
        grantAttributePermission(authorityName, "department.costCenter", "VIEW");
        grantAttributePermission(authorityName, "department.employees", "VIEW");
        grantAttributePermission(authorityName, "employee.employeeNumber", "VIEW");
        grantAttributePermission(authorityName, "employee.firstName", "VIEW");
        grantAttributePermission(authorityName, "employee.lastName", "VIEW");
        grantAttributePermission(authorityName, "employee.email", "VIEW");
    }

    private void grantEditableOrganizationFields(String authorityName) throws Exception {
        grantAttributePermission(authorityName, "organization.code", "EDIT");
        grantAttributePermission(authorityName, "organization.name", "EDIT");
        grantAttributePermission(authorityName, "organization.ownerLogin", "EDIT");
    }

    private void grantAttributePermission(String authorityName, String target, String action) throws Exception {
        Map<String, Object> permissionPayload = Map.of(
            "authorityName",
            authorityName,
            "targetType",
            "ATTRIBUTE",
            "target",
            target,
            "action",
            action,
            "effect",
            "GRANT"
        );

        restMockMvc
            .perform(
                post(PERMISSION_ADMIN_API_URL)
                    .with(user("admin").authorities(new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(permissionPayload))
            )
            .andExpect(status().isCreated());
    }
}
