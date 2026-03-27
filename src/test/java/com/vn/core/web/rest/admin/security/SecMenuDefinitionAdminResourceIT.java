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
import com.vn.core.security.domain.SecMenuDefinition;
import com.vn.core.security.domain.SecMenuPermission;
import com.vn.core.security.repository.SecMenuDefinitionRepository;
import com.vn.core.security.repository.SecMenuPermissionRepository;
import com.vn.core.service.dto.security.SecMenuDefinitionDTO;
import com.vn.core.service.dto.security.SyncNodeDTO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link SecMenuDefinitionAdminResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class SecMenuDefinitionAdminResourceIT {

    private static final String ENTITY_API_URL = "/api/admin/sec/menu-definitions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String SYNC_URL = ENTITY_API_URL + "/sync";

    private static final String DEFAULT_APP = "jhipster-security-platform";
    private static final String DEFAULT_MENU_ID = "test-menu-home";
    private static final String DEFAULT_MENU_NAME = "Home Menu";
    private static final String DEFAULT_LABEL = "Home";
    private static final String UPDATED_LABEL = "Updated Home";
    private static final Integer DEFAULT_ORDERING = 1;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private MockMvc restMockMvc;

    @Autowired
    private SecMenuDefinitionRepository secMenuDefinitionRepository;

    @Autowired
    private SecMenuPermissionRepository secMenuPermissionRepository;

    @Test
    void createMenuDefinition_returns201() throws Exception {
        SecMenuDefinitionDTO dto = createDto(DEFAULT_MENU_ID, DEFAULT_APP, DEFAULT_MENU_NAME, DEFAULT_LABEL, DEFAULT_ORDERING);

        String response = restMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.menuId").value(DEFAULT_MENU_ID))
            .andExpect(jsonPath("$.appName").value(DEFAULT_APP))
            .andExpect(jsonPath("$.label").value(DEFAULT_LABEL))
            .andReturn()
            .getResponse()
            .getContentAsString();

        SecMenuDefinitionDTO created = om.readValue(response, SecMenuDefinitionDTO.class);
        assertThat(created.getId()).isNotNull();

        SecMenuDefinition persisted = secMenuDefinitionRepository.findById(created.getId()).orElseThrow();
        assertThat(persisted.getMenuId()).isEqualTo(DEFAULT_MENU_ID);
        assertThat(persisted.getLabel()).isEqualTo(DEFAULT_LABEL);
    }

    @Test
    void createMenuDefinition_duplicateMenuId_returns400() throws Exception {
        secMenuDefinitionRepository.saveAndFlush(createEntity(DEFAULT_MENU_ID, DEFAULT_APP));

        SecMenuDefinitionDTO dto = createDto(DEFAULT_MENU_ID, DEFAULT_APP, DEFAULT_MENU_NAME, DEFAULT_LABEL, DEFAULT_ORDERING);

        restMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.menuIdExists"));
    }

    @Test
    void getAllMenuDefinitions_returns200() throws Exception {
        secMenuDefinitionRepository.saveAndFlush(createEntity(DEFAULT_MENU_ID, DEFAULT_APP));

        restMockMvc
            .perform(get(ENTITY_API_URL + "?appName=" + DEFAULT_APP))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].menuId").value(hasItem(DEFAULT_MENU_ID)));
    }

    @Test
    void getAllMenuDefinitions_withoutAppFilter_returnsDefinitionsAcrossApps() throws Exception {
        secMenuDefinitionRepository.saveAndFlush(createEntity(DEFAULT_MENU_ID, DEFAULT_APP));
        secMenuDefinitionRepository.saveAndFlush(createEntity("sales-home", "sales-console"));

        restMockMvc
            .perform(get(ENTITY_API_URL))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$.[*].appName").value(hasItem(DEFAULT_APP)))
            .andExpect(jsonPath("$.[*].appName").value(hasItem("sales-console")))
            .andExpect(jsonPath("$.[*].menuId").value(hasItem(DEFAULT_MENU_ID)))
            .andExpect(jsonPath("$.[*].menuId").value(hasItem("sales-home")));
    }

    @Test
    void getMenuDefinition_notFound_returns404() throws Exception {
        restMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    void updateMenuDefinition_returns200() throws Exception {
        SecMenuDefinition saved = secMenuDefinitionRepository.saveAndFlush(createEntity(DEFAULT_MENU_ID, DEFAULT_APP));

        SecMenuDefinitionDTO dto = createDto(DEFAULT_MENU_ID, DEFAULT_APP, DEFAULT_MENU_NAME, UPDATED_LABEL, DEFAULT_ORDERING);
        dto.setId(saved.getId());

        restMockMvc
            .perform(put(ENTITY_API_URL_ID, saved.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.label").value(UPDATED_LABEL));

        SecMenuDefinition persisted = secMenuDefinitionRepository.findById(saved.getId()).orElseThrow();
        assertThat(persisted.getLabel()).isEqualTo(UPDATED_LABEL);
    }

    @Test
    void updateMenuDefinition_idMismatch_returns400() throws Exception {
        SecMenuDefinition saved = secMenuDefinitionRepository.saveAndFlush(createEntity(DEFAULT_MENU_ID, DEFAULT_APP));

        SecMenuDefinitionDTO dto = createDto(DEFAULT_MENU_ID, DEFAULT_APP, DEFAULT_MENU_NAME, DEFAULT_LABEL, DEFAULT_ORDERING);
        dto.setId(saved.getId() + 999L);

        restMockMvc
            .perform(put(ENTITY_API_URL_ID, saved.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.idMismatch"));
    }

    @Test
    void deleteMenuDefinition_cascadesPermissions() throws Exception {
        SecMenuDefinition saved = secMenuDefinitionRepository.saveAndFlush(createEntity(DEFAULT_MENU_ID, DEFAULT_APP));
        SecMenuPermission perm = secMenuPermissionRepository.saveAndFlush(
            new SecMenuPermission().role(AuthoritiesConstants.ADMIN).appName(DEFAULT_APP).menuId(DEFAULT_MENU_ID).effect("ALLOW")
        );

        restMockMvc.perform(delete(ENTITY_API_URL_ID, saved.getId())).andExpect(status().isNoContent());

        assertThat(secMenuDefinitionRepository.findById(saved.getId())).isEmpty();
        assertThat(secMenuPermissionRepository.findById(perm.getId())).isEmpty();
    }

    @Test
    void syncFromRegistry_insertsNewSkipsExisting() throws Exception {
        secMenuDefinitionRepository.saveAndFlush(createEntity("existing-menu", DEFAULT_APP));

        SyncNodeDTO existing = createSyncNode("existing-menu", DEFAULT_APP);
        SyncNodeDTO newNode = createSyncNode("new-menu", DEFAULT_APP);

        restMockMvc
            .perform(post(SYNC_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(List.of(existing, newNode))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.seeded").value(1))
            .andExpect(jsonPath("$.skipped").value(1));

        assertThat(secMenuDefinitionRepository.findByAppNameAndMenuId(DEFAULT_APP, "new-menu")).isPresent();
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void nonAdmin_returns403() throws Exception {
        restMockMvc.perform(get(ENTITY_API_URL)).andExpect(status().isForbidden());
    }

    private static SecMenuDefinition createEntity(String menuId, String appName) {
        return new SecMenuDefinition().menuId(menuId).appName(appName).menuName("Menu " + menuId).label("Label " + menuId).ordering(1);
    }

    private static SecMenuDefinitionDTO createDto(String menuId, String appName, String menuName, String label, Integer ordering) {
        SecMenuDefinitionDTO dto = new SecMenuDefinitionDTO();
        dto.setMenuId(menuId);
        dto.setAppName(appName);
        dto.setMenuName(menuName);
        dto.setLabel(label);
        dto.setOrdering(ordering);
        return dto;
    }

    private static SyncNodeDTO createSyncNode(String menuId, String appName) {
        SyncNodeDTO node = new SyncNodeDTO();
        node.setMenuId(menuId);
        node.setAppName(appName);
        node.setMenuName("Sync Menu " + menuId);
        node.setLabel("Sync Label " + menuId);
        node.setOrdering(1);
        return node;
    }
}
