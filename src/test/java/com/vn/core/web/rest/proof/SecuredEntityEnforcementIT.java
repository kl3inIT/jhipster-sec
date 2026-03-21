package com.vn.core.web.rest.proof;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.core.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests proving allow and deny behavior for the proof entities.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class SecuredEntityEnforcementIT {

    private static final String ORGANIZATION_API_URL = "/api/proof/organizations";
    private static final String DEPARTMENT_API_URL = "/api/proof/departments";
    private static final String EMPLOYEE_API_URL = "/api/proof/employees";

    @Autowired
    private MockMvc restMockMvc;

    @Autowired
    private ObjectMapper om;

    @Test
    @WithMockUser(username = "proof-owner", authorities = "ROLE_PROOF_READER")
    void getOrganizations_returnsOwnedRowsOnly() throws Exception {
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
    @WithMockUser(username = "proof-owner", authorities = "ROLE_PROOF_READER")
    void getOrganization_outsideRowPolicy_returnsNotFound() throws Exception {
        restMockMvc.perform(get(ORGANIZATION_API_URL + "/{id}", 101)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "proof-owner", authorities = "ROLE_PROOF_EDITOR")
    void createOrganization_withEditorRole_returnsCreated() throws Exception {
        Map<String, Object> payload = Map.of("code", "ORG-NEW", "name", "New Org", "ownerLogin", "proof-owner");

        restMockMvc
            .perform(post(ORGANIZATION_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(payload)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("ORG-NEW"))
            .andExpect(jsonPath("$.name").value("New Org"));
    }

    @Test
    @WithMockUser(username = "proof-owner", authorities = "ROLE_PROOF_EDITOR")
    void updateOrganization_withDeniedBudgetEdit_returnsForbidden() throws Exception {
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
}
