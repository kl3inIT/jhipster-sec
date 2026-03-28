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
import com.vn.core.security.domain.SecPermission;
import com.vn.core.security.permission.TargetType;
import com.vn.core.security.repository.SecPermissionRepository;
import com.vn.core.service.dto.security.SecPermissionDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link SecPermissionAdminResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class SecPermissionAdminResourceIT {

    private static final String ENTITY_API_URL = "/api/admin/sec/permissions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final String DEFAULT_AUTHORITY_NAME = AuthoritiesConstants.ADMIN;
    private static final String MATRIX_AUTHORITY_NAME = "ROLE_PROOF_NONE";
    private static final String DEFAULT_TARGET_TYPE = "ENTITY";
    private static final String DEFAULT_TARGET = "Organization";
    private static final String UPDATED_TARGET = "Department";
    private static final String NORMALIZED_DEFAULT_TARGET = "ORGANIZATION";
    private static final String NORMALIZED_UPDATED_TARGET = "DEPARTMENT";
    private static final String DEFAULT_ACTION = "READ";
    private static final String DEFAULT_EFFECT = "ALLOW";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private MockMvc restMockMvc;

    @Autowired
    private SecPermissionRepository secPermissionRepository;

    @Test
    void testCreatePermission() throws Exception {
        SecPermissionDTO dto = createPermissionDto();

        String response = restMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.authorityName").value(DEFAULT_AUTHORITY_NAME))
            .andExpect(jsonPath("$.targetType").value(DEFAULT_TARGET_TYPE))
            .andExpect(jsonPath("$.target").value(NORMALIZED_DEFAULT_TARGET))
            .andExpect(jsonPath("$.action").value(DEFAULT_ACTION))
            .andExpect(jsonPath("$.effect").value(DEFAULT_EFFECT))
            .andReturn()
            .getResponse()
            .getContentAsString();

        SecPermissionDTO created = om.readValue(response, SecPermissionDTO.class);
        SecPermission persisted = secPermissionRepository.findById(created.getId()).orElseThrow();
        assertThat(persisted.getAuthorityName()).isEqualTo(DEFAULT_AUTHORITY_NAME);
        assertThat(persisted.getTargetType()).isEqualTo(TargetType.ENTITY);
        assertThat(persisted.getTarget()).isEqualTo(NORMALIZED_DEFAULT_TARGET);
    }

    @Test
    void testCreatePermissionWithExistingId() throws Exception {
        SecPermissionDTO dto = createPermissionDto();
        dto.setId(1L);

        restMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.idexists"));
    }

    @Test
    void testCreatePermissionWithInvalidRole() throws Exception {
        SecPermissionDTO dto = createPermissionDto();
        dto.setAuthorityName("ROLE_DOES_NOT_EXIST");

        restMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.rolenotfound"));
    }

    @Test
    void testGetAllPermissions() throws Exception {
        secPermissionRepository.saveAndFlush(createPermissionEntity().target("OrganizationList"));

        restMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,asc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].target").value(hasItem("OrganizationList")))
            .andExpect(jsonPath("$.[*].authorityName").value(hasItem(DEFAULT_AUTHORITY_NAME)));
    }

    @Test
    void testGetPermission() throws Exception {
        SecPermission inserted = secPermissionRepository.saveAndFlush(createPermissionEntity().target("OrganizationGet"));

        restMockMvc
            .perform(get(ENTITY_API_URL_ID, inserted.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(inserted.getId()))
            .andExpect(jsonPath("$.target").value("OrganizationGet"))
            .andExpect(jsonPath("$.authorityName").value(DEFAULT_AUTHORITY_NAME));
    }

    @Test
    void createPermission_matrixEntityGrantStoresAllowAndRoundTripsAsGrant() throws Exception {
        SecPermissionDTO created = createPermission(createMatrixPermissionDto("ENTITY", "organization", "READ"));

        SecPermission persisted = secPermissionRepository.findById(created.getId()).orElseThrow();
        assertThat(persisted.getTarget()).isEqualTo("ORGANIZATION");
        assertThat(persisted.getEffect()).isEqualTo("ALLOW");

        restMockMvc
            .perform(get(ENTITY_API_URL + "?authorityName=" + MATRIX_AUTHORITY_NAME))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].target").value("organization"))
            .andExpect(jsonPath("$[0].effect").value("GRANT"));

        restMockMvc
            .perform(get(ENTITY_API_URL_ID, created.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.target").value("organization"))
            .andExpect(jsonPath("$.effect").value("GRANT"));
    }

    @Test
    void createPermission_matrixAttributeGrantStoresAllowAndRoundTripsAsGrant() throws Exception {
        SecPermissionDTO created = createPermission(createMatrixPermissionDto("ATTRIBUTE", "organization.budget", "VIEW"));

        SecPermission persisted = secPermissionRepository.findById(created.getId()).orElseThrow();
        assertThat(persisted.getTarget()).isEqualTo("ORGANIZATION.BUDGET");
        assertThat(persisted.getEffect()).isEqualTo("ALLOW");

        restMockMvc
            .perform(get(ENTITY_API_URL + "?authorityName=" + MATRIX_AUTHORITY_NAME))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].target").value("organization.budget"))
            .andExpect(jsonPath("$[0].effect").value("GRANT"));

        restMockMvc
            .perform(get(ENTITY_API_URL_ID, created.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.target").value("organization.budget"))
            .andExpect(jsonPath("$.effect").value("GRANT"));
    }

    @Test
    void createPermission_matrixAttributeGrantForSecondEntityStoresAllowAndRoundTripsAsGrant() throws Exception {
        SecPermissionDTO created = createPermission(createMatrixPermissionDto("ATTRIBUTE", "employee.salary", "VIEW"));

        SecPermission persisted = secPermissionRepository.findById(created.getId()).orElseThrow();
        assertThat(persisted.getTarget()).isEqualTo("EMPLOYEE.SALARY");
        assertThat(persisted.getEffect()).isEqualTo("ALLOW");

        restMockMvc
            .perform(get(ENTITY_API_URL + "?authorityName=" + MATRIX_AUTHORITY_NAME))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].target").value("employee.salary"))
            .andExpect(jsonPath("$[0].effect").value("GRANT"));

        restMockMvc
            .perform(get(ENTITY_API_URL_ID, created.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.target").value("employee.salary"))
            .andExpect(jsonPath("$.effect").value("GRANT"));
    }

    @Test
    void getPermissions_camelCaseAttributeRoundTripsWithOriginalCasing() throws Exception {
        // Regression for storedToUiTargets() returning lowercase key instead of original camelCase.
        // DEPARTMENT.COSTCENTER stored -> must come back as "department.costCenter" (not "department.costcenter")
        // so the frontend Map lookup keyed by catalog-derived "department.costCenter" succeeds.
        SecPermissionDTO created = createPermission(createMatrixPermissionDto("ATTRIBUTE", "department.costCenter", "VIEW"));

        SecPermission persisted = secPermissionRepository.findById(created.getId()).orElseThrow();
        assertThat(persisted.getTarget()).isEqualTo("DEPARTMENT.COSTCENTER");

        restMockMvc
            .perform(get(ENTITY_API_URL + "?authorityName=" + MATRIX_AUTHORITY_NAME))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].target").value("department.costCenter"));

        restMockMvc
            .perform(get(ENTITY_API_URL_ID, created.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.target").value("department.costCenter"));
    }

    @Test
    void createPermission_matrixGrantReturnsExistingRowInsteadOfCreatingDuplicate() throws Exception {
        SecPermission existing = secPermissionRepository.saveAndFlush(
            new SecPermission()
                .authorityName(MATRIX_AUTHORITY_NAME)
                .targetType(TargetType.ENTITY)
                .target("ORGANIZATION")
                .action("READ")
                .effect("ALLOW")
        );

        restMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(createMatrixPermissionDto("ENTITY", "organization", "READ")))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(existing.getId()))
            .andExpect(jsonPath("$.target").value("ORGANIZATION"))
            .andExpect(jsonPath("$.effect").value("ALLOW"));

        assertThat(
            secPermissionRepository.findAllByAuthorityNameAndTargetTypeAndTargetAndActionOrderByIdAsc(
                MATRIX_AUTHORITY_NAME,
                TargetType.ENTITY,
                "ORGANIZATION",
                "READ"
            )
        ).hasSize(1);
    }

    @Test
    void testGetNonExistingPermission() throws Exception {
        restMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    void testUpdatePermission() throws Exception {
        SecPermission inserted = secPermissionRepository.saveAndFlush(createPermissionEntity());
        SecPermissionDTO dto = createPermissionDto();
        dto.setId(inserted.getId());
        dto.setTarget(UPDATED_TARGET);

        restMockMvc
            .perform(put(ENTITY_API_URL_ID, inserted.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(inserted.getId()))
            .andExpect(jsonPath("$.target").value(NORMALIZED_UPDATED_TARGET));

        SecPermission persisted = secPermissionRepository.findById(inserted.getId()).orElseThrow();
        assertThat(persisted.getTarget()).isEqualTo(NORMALIZED_UPDATED_TARGET);
    }

    @Test
    void testDeletePermission() throws Exception {
        SecPermission inserted = secPermissionRepository.saveAndFlush(createPermissionEntity());

        restMockMvc.perform(delete(ENTITY_API_URL_ID, inserted.getId())).andExpect(status().isNoContent());

        assertThat(secPermissionRepository.findById(inserted.getId())).isEmpty();
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void testNonAdminCannotAccessPermissions() throws Exception {
        restMockMvc.perform(get(ENTITY_API_URL)).andExpect(status().isForbidden());
    }

    @Test
    void getAllPermissions_filterByAuthorityName() throws Exception {
        secPermissionRepository.saveAndFlush(
            new SecPermission()
                .authorityName(AuthoritiesConstants.ADMIN)
                .targetType(TargetType.ENTITY)
                .target("OrganizationFilter")
                .action("READ")
                .effect("ALLOW")
        );
        secPermissionRepository.saveAndFlush(
            new SecPermission()
                .authorityName(AuthoritiesConstants.USER)
                .targetType(TargetType.ENTITY)
                .target("OrganizationFilterUser")
                .action("READ")
                .effect("ALLOW")
        );

        restMockMvc
            .perform(get(ENTITY_API_URL + "?authorityName=" + AuthoritiesConstants.ADMIN))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(
                jsonPath("$[*].authorityName").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(AuthoritiesConstants.ADMIN)))
            )
            .andExpect(
                jsonPath("$[*].authorityName").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(AuthoritiesConstants.USER)))
            );
    }

    @Test
    void getAllPermissions_noFilterReturnsAll() throws Exception {
        secPermissionRepository.saveAndFlush(
            new SecPermission()
                .authorityName(AuthoritiesConstants.ADMIN)
                .targetType(TargetType.ENTITY)
                .target("OrganizationAll1")
                .action("READ")
                .effect("ALLOW")
        );
        secPermissionRepository.saveAndFlush(
            new SecPermission()
                .authorityName(AuthoritiesConstants.USER)
                .targetType(TargetType.ENTITY)
                .target("OrganizationAll2")
                .action("READ")
                .effect("ALLOW")
        );

        int filteredCount =
            restMockMvc
                .perform(get(ENTITY_API_URL + "?authorityName=" + AuthoritiesConstants.ADMIN))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .split("authorityName")
                .length -
            1;

        String allResponse = restMockMvc
            .perform(get(ENTITY_API_URL))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        int totalCount = allResponse.split("authorityName").length - 1;
        org.assertj.core.api.Assertions.assertThat(totalCount).isGreaterThanOrEqualTo(filteredCount);
    }

    @Test
    void testCreatePermissionWithInvalidData() throws Exception {
        SecPermissionDTO dto = createPermissionDto();
        dto.setTarget(" ");
        dto.setTargetType(null);

        restMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.validation"))
            .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    private static SecPermission createPermissionEntity() {
        return new SecPermission()
            .authorityName(DEFAULT_AUTHORITY_NAME)
            .targetType(TargetType.ENTITY)
            .target(DEFAULT_TARGET)
            .action(DEFAULT_ACTION)
            .effect(DEFAULT_EFFECT);
    }

    private static SecPermissionDTO createPermissionDto() {
        SecPermissionDTO dto = new SecPermissionDTO();
        dto.setAuthorityName(DEFAULT_AUTHORITY_NAME);
        dto.setTargetType(DEFAULT_TARGET_TYPE);
        dto.setTarget(DEFAULT_TARGET);
        dto.setAction(DEFAULT_ACTION);
        dto.setEffect(DEFAULT_EFFECT);
        return dto;
    }

    private SecPermissionDTO createPermission(SecPermissionDTO dto) throws Exception {
        String response = restMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return om.readValue(response, SecPermissionDTO.class);
    }

    private static SecPermissionDTO createMatrixPermissionDto(String targetType, String target, String action) {
        SecPermissionDTO dto = new SecPermissionDTO();
        dto.setAuthorityName(MATRIX_AUTHORITY_NAME);
        dto.setTargetType(targetType);
        dto.setTarget(target);
        dto.setAction(action);
        dto.setEffect("GRANT");
        return dto;
    }
}
