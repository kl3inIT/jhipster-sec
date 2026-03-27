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
import com.vn.core.domain.Authority;
import com.vn.core.domain.RoleType;
import com.vn.core.repository.AuthorityRepository;
import com.vn.core.security.AuthoritiesConstants;
import com.vn.core.security.domain.SecPermission;
import com.vn.core.security.permission.TargetType;
import com.vn.core.security.repository.SecPermissionRepository;
import com.vn.core.service.dto.security.SecRoleDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link SecRoleAdminResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class SecRoleAdminResourceIT {

    private static final String ENTITY_API_URL = "/api/admin/sec/roles";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{name}";

    private static final String DEFAULT_NAME = "TEST_ROLE";
    private static final String DEFAULT_DISPLAY_NAME = "Test Role";
    private static final String DEFAULT_TYPE = "RESOURCE";
    private static final String UPDATED_DISPLAY_NAME = "Updated Test Role";
    private static final String UPDATED_TYPE = "RESOURCE";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private MockMvc restMockMvc;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private SecPermissionRepository secPermissionRepository;

    @Test
    void testCreateRole() throws Exception {
        SecRoleDTO dto = createRoleDto(DEFAULT_NAME, DEFAULT_DISPLAY_NAME, DEFAULT_TYPE);

        restMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.displayName").value(DEFAULT_DISPLAY_NAME))
            .andExpect(jsonPath("$.type").value(DEFAULT_TYPE));

        Authority persisted = authorityRepository.findById(DEFAULT_NAME).orElseThrow();
        assertThat(persisted.getDisplayName()).isEqualTo(DEFAULT_DISPLAY_NAME);
        assertThat(persisted.getType()).isEqualTo(RoleType.RESOURCE);
    }

    @Test
    void testCreateRoleWithExistingName() throws Exception {
        SecRoleDTO dto = createRoleDto(AuthoritiesConstants.ADMIN, "Duplicate Admin", DEFAULT_TYPE);

        restMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.nameexists"));
    }

    @Test
    void testGetAllRoles() throws Exception {
        authorityRepository.saveAndFlush(createAuthority("TEST_ROLE_LIST", "List Role", RoleType.RESOURCE));

        restMockMvc
            .perform(get(ENTITY_API_URL + "?sort=name,asc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].name").value(hasItem("TEST_ROLE_LIST")))
            .andExpect(jsonPath("$.[*].displayName").value(hasItem("List Role")));
    }

    @Test
    void testGetRole() throws Exception {
        authorityRepository.saveAndFlush(createAuthority("TEST_ROLE_GET", "Get Role", RoleType.RESOURCE));

        restMockMvc
            .perform(get(ENTITY_API_URL_ID, "TEST_ROLE_GET"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.name").value("TEST_ROLE_GET"))
            .andExpect(jsonPath("$.displayName").value("Get Role"))
            .andExpect(jsonPath("$.type").value("RESOURCE"));
    }

    @Test
    void testGetNonExistingRole() throws Exception {
        restMockMvc.perform(get(ENTITY_API_URL_ID, "ROLE_DOES_NOT_EXIST")).andExpect(status().isNotFound());
    }

    @Test
    void testUpdateRole() throws Exception {
        authorityRepository.saveAndFlush(createAuthority(DEFAULT_NAME, DEFAULT_DISPLAY_NAME, RoleType.RESOURCE));
        SecRoleDTO dto = createRoleDto(DEFAULT_NAME, UPDATED_DISPLAY_NAME, UPDATED_TYPE);

        restMockMvc
            .perform(put(ENTITY_API_URL_ID, DEFAULT_NAME).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.displayName").value(UPDATED_DISPLAY_NAME))
            .andExpect(jsonPath("$.type").value(UPDATED_TYPE));

        Authority persisted = authorityRepository.findById(DEFAULT_NAME).orElseThrow();
        assertThat(persisted.getDisplayName()).isEqualTo(UPDATED_DISPLAY_NAME);
        assertThat(persisted.getType()).isEqualTo(RoleType.RESOURCE);
    }

    @Test
    void testUpdateRoleNameMismatch() throws Exception {
        authorityRepository.saveAndFlush(createAuthority("TEST_ROLE_MISMATCH", "Mismatch", RoleType.RESOURCE));
        SecRoleDTO dto = createRoleDto("TEST_ROLE_OTHER", "Other", DEFAULT_TYPE);

        restMockMvc
            .perform(
                put(ENTITY_API_URL_ID, "TEST_ROLE_MISMATCH").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.namemismatch"));
    }

    @Test
    void testDeleteRole() throws Exception {
        authorityRepository.saveAndFlush(createAuthority("TEST_ROLE_DELETE", "Delete Role", RoleType.RESOURCE));
        secPermissionRepository.saveAndFlush(
            new SecPermission()
                .authorityName("TEST_ROLE_DELETE")
                .targetType(TargetType.ENTITY)
                .target("Organization")
                .action("READ")
                .effect("ALLOW")
        );

        restMockMvc.perform(delete(ENTITY_API_URL_ID, "TEST_ROLE_DELETE")).andExpect(status().isNoContent());

        assertThat(authorityRepository.findById("TEST_ROLE_DELETE")).isEmpty();
        assertThat(secPermissionRepository.findByAuthorityName("TEST_ROLE_DELETE")).isEmpty();
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void testNonAdminCannotAccessRoles() throws Exception {
        restMockMvc.perform(get(ENTITY_API_URL)).andExpect(status().isForbidden());
    }

    @Test
    void testCreateRoleWithInvalidData() throws Exception {
        SecRoleDTO dto = createRoleDto(" ", DEFAULT_DISPLAY_NAME, null);

        restMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.validation"))
            .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    private static Authority createAuthority(String name, String displayName, RoleType type) {
        return new Authority().name(name).displayName(displayName).type(type);
    }

    private static SecRoleDTO createRoleDto(String name, String displayName, String type) {
        return new SecRoleDTO(name, displayName, type);
    }
}
