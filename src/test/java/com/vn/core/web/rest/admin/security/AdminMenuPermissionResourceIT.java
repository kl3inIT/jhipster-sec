package com.vn.core.web.rest.admin.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.core.IntegrationTest;
import com.vn.core.security.AuthoritiesConstants;
import com.vn.core.security.domain.SecMenuPermission;
import com.vn.core.security.repository.SecMenuPermissionRepository;
import com.vn.core.service.dto.security.SecMenuPermissionDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link AdminMenuPermissionResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class AdminMenuPermissionResourceIT {

    private static final String ENTITY_API_URL = "/api/admin/sec/menu-permissions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final String DEFAULT_APP = "jhipster-security-platform";
    private static final String DEFAULT_ROLE = AuthoritiesConstants.ADMIN;
    private static final String OTHER_ROLE = AuthoritiesConstants.USER;
    private static final String DEFAULT_MENU_ID = "test-menu-perm";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private MockMvc restMockMvc;

    @Autowired
    private SecMenuPermissionRepository secMenuPermissionRepository;

    @Test
    void createMenuPermission_returns201() throws Exception {
        SecMenuPermissionDTO dto = createDto(DEFAULT_ROLE, DEFAULT_APP, DEFAULT_MENU_ID, "ALLOW");

        String response = restMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value(DEFAULT_ROLE))
            .andExpect(jsonPath("$.menuId").value(DEFAULT_MENU_ID))
            .andExpect(jsonPath("$.effect").value("ALLOW"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        SecMenuPermissionDTO created = om.readValue(response, SecMenuPermissionDTO.class);
        assertThat(created.getId()).isNotNull();

        SecMenuPermission persisted = secMenuPermissionRepository.findById(created.getId()).orElseThrow();
        assertThat(persisted.getRole()).isEqualTo(DEFAULT_ROLE);
        assertThat(persisted.getEffect()).isEqualTo("ALLOW");
    }

    @Test
    void createMenuPermission_roleNotFound_returns400() throws Exception {
        SecMenuPermissionDTO dto = createDto("ROLE_DOES_NOT_EXIST", DEFAULT_APP, DEFAULT_MENU_ID, "ALLOW");

        restMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.roleNotFound"));
    }

    @Test
    void queryByRole_returns200() throws Exception {
        secMenuPermissionRepository.saveAndFlush(
            new SecMenuPermission().role(DEFAULT_ROLE).appName(DEFAULT_APP).menuId("menu-admin-1").effect("ALLOW")
        );
        secMenuPermissionRepository.saveAndFlush(
            new SecMenuPermission().role(OTHER_ROLE).appName(DEFAULT_APP).menuId("menu-user-1").effect("ALLOW")
        );

        restMockMvc
            .perform(get(ENTITY_API_URL + "?role=" + DEFAULT_ROLE + "&appName=" + DEFAULT_APP))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$[*].role").value(everyItem(is(DEFAULT_ROLE))));
    }

    @Test
    void deleteMenuPermission_returns204() throws Exception {
        SecMenuPermission saved = secMenuPermissionRepository.saveAndFlush(
            new SecMenuPermission().role(DEFAULT_ROLE).appName(DEFAULT_APP).menuId("menu-delete-test").effect("ALLOW")
        );

        restMockMvc.perform(delete(ENTITY_API_URL_ID, saved.getId())).andExpect(status().isNoContent());

        assertThat(secMenuPermissionRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void nonAdmin_returns403() throws Exception {
        restMockMvc.perform(get(ENTITY_API_URL + "?role=" + DEFAULT_ROLE)).andExpect(status().isForbidden());
    }

    private static SecMenuPermissionDTO createDto(String role, String appName, String menuId, String effect) {
        SecMenuPermissionDTO dto = new SecMenuPermissionDTO();
        dto.setRole(role);
        dto.setAppName(appName);
        dto.setMenuId(menuId);
        dto.setEffect(effect);
        return dto;
    }
}
