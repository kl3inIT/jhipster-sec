package com.vn.core.web.rest.admin.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.core.IntegrationTest;
import com.vn.core.security.AuthoritiesConstants;
import com.vn.core.security.domain.SecRowPolicy;
import com.vn.core.security.repository.SecRowPolicyRepository;
import com.vn.core.service.dto.security.SecRowPolicyDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link SecRowPolicyAdminResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class SecRowPolicyAdminResourceIT {

    private static final String ENTITY_API_URL = "/api/admin/sec/row-policies";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final String DEFAULT_CODE = "test-policy-code";
    private static final String DEFAULT_ENTITY_NAME = "Organization";
    private static final String DEFAULT_OPERATION = "READ";
    private static final String DEFAULT_POLICY_TYPE = "SPECIFICATION";
    private static final String DEFAULT_EXPRESSION = "owner = :currentUser";
    private static final String UPDATED_EXPRESSION = "department = :currentDepartment";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private MockMvc restMockMvc;

    @Autowired
    private SecRowPolicyRepository secRowPolicyRepository;

    @Test
    void testCreateRowPolicy() throws Exception {
        SecRowPolicyDTO dto = createRowPolicyDto();

        String response = restMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE))
            .andExpect(jsonPath("$.entityName").value(DEFAULT_ENTITY_NAME))
            .andExpect(jsonPath("$.operation").value(DEFAULT_OPERATION))
            .andExpect(jsonPath("$.policyType").value(DEFAULT_POLICY_TYPE))
            .andExpect(jsonPath("$.expression").value(DEFAULT_EXPRESSION))
            .andReturn()
            .getResponse()
            .getContentAsString();

        SecRowPolicyDTO created = om.readValue(response, SecRowPolicyDTO.class);
        SecRowPolicy persisted = secRowPolicyRepository.findById(created.getId()).orElseThrow();
        assertThat(persisted.getCode()).isEqualTo(DEFAULT_CODE);
        assertThat(persisted.getExpression()).isEqualTo(DEFAULT_EXPRESSION);
    }

    @Test
    void testCreateRowPolicyWithExistingId() throws Exception {
        SecRowPolicyDTO dto = createRowPolicyDto();
        dto.setId(1L);

        restMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.idexists"));
    }

    @Test
    void testCreateRowPolicyWithDuplicateCode() throws Exception {
        secRowPolicyRepository.saveAndFlush(createRowPolicyEntity());
        SecRowPolicyDTO dto = createRowPolicyDto();

        restMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.codeexists"));
    }

    @Test
    void testGetAllRowPolicies() throws Exception {
        secRowPolicyRepository.saveAndFlush(createRowPolicyEntity().code("list-policy-code"));

        restMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,asc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].code").value(hasItem("list-policy-code")))
            .andExpect(jsonPath("$.[*].entityName").value(hasItem(DEFAULT_ENTITY_NAME)));
    }

    @Test
    void testGetRowPolicy() throws Exception {
        SecRowPolicy inserted = secRowPolicyRepository.saveAndFlush(createRowPolicyEntity().code("get-policy-code"));

        restMockMvc
            .perform(get(ENTITY_API_URL_ID, inserted.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(inserted.getId()))
            .andExpect(jsonPath("$.code").value("get-policy-code"))
            .andExpect(jsonPath("$.expression").value(DEFAULT_EXPRESSION));
    }

    @Test
    void testGetNonExistingRowPolicy() throws Exception {
        restMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    void testUpdateRowPolicy() throws Exception {
        SecRowPolicy inserted = secRowPolicyRepository.saveAndFlush(createRowPolicyEntity());
        SecRowPolicyDTO dto = createRowPolicyDto();
        dto.setId(inserted.getId());
        dto.setExpression(UPDATED_EXPRESSION);

        restMockMvc
            .perform(put(ENTITY_API_URL_ID, inserted.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(inserted.getId()))
            .andExpect(jsonPath("$.expression").value(UPDATED_EXPRESSION));

        SecRowPolicy persisted = secRowPolicyRepository.findById(inserted.getId()).orElseThrow();
        assertThat(persisted.getExpression()).isEqualTo(UPDATED_EXPRESSION);
    }

    @Test
    void testUpdateRowPolicyCodeConflict() throws Exception {
        SecRowPolicy first = secRowPolicyRepository.saveAndFlush(createRowPolicyEntity().code("conflict-policy-a"));
        secRowPolicyRepository.saveAndFlush(createRowPolicyEntity().code("conflict-policy-b"));
        SecRowPolicyDTO dto = createRowPolicyDto();
        dto.setId(first.getId());
        dto.setCode("conflict-policy-b");

        restMockMvc
            .perform(put(ENTITY_API_URL_ID, first.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.codeexists"));
    }

    @Test
    void testDeleteRowPolicy() throws Exception {
        SecRowPolicy inserted = secRowPolicyRepository.saveAndFlush(createRowPolicyEntity());

        restMockMvc.perform(delete(ENTITY_API_URL_ID, inserted.getId())).andExpect(status().isNoContent());

        assertThat(secRowPolicyRepository.findById(inserted.getId())).isEmpty();
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void testNonAdminCannotAccessRowPolicies() throws Exception {
        restMockMvc.perform(get(ENTITY_API_URL)).andExpect(status().isForbidden());
    }

    @Test
    void testCreateRowPolicyWithInvalidData() throws Exception {
        SecRowPolicyDTO dto = createRowPolicyDto();
        dto.setCode(" ");
        dto.setPolicyType(null);

        restMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.validation"))
            .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    private static SecRowPolicy createRowPolicyEntity() {
        return new SecRowPolicy()
            .code(DEFAULT_CODE)
            .entityName(DEFAULT_ENTITY_NAME)
            .operation(DEFAULT_OPERATION)
            .policyType(DEFAULT_POLICY_TYPE)
            .expression(DEFAULT_EXPRESSION);
    }

    private static SecRowPolicyDTO createRowPolicyDto() {
        SecRowPolicyDTO dto = new SecRowPolicyDTO();
        dto.setCode(DEFAULT_CODE);
        dto.setEntityName(DEFAULT_ENTITY_NAME);
        dto.setOperation(DEFAULT_OPERATION);
        dto.setPolicyType(DEFAULT_POLICY_TYPE);
        dto.setExpression(DEFAULT_EXPRESSION);
        return dto;
    }
}
