package com.vn.core.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.core.IntegrationTest;
import com.vn.core.security.AuthoritiesConstants;
import com.vn.core.service.dto.security.SecMenuPermissionDTO;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link MenuPermissionResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class MenuPermissionResourceIT {

    private static final String APP_NAME = "jhipster-security-platform";
    private static final String ENTITY_API_URL = "/api/security/menu-permissions";
    private static final String ADMIN_ENTITY_API_URL = "/api/admin/sec/menu-permissions";

    @Autowired
    private MockMvc restMockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getMenuPermissions_returnsUnionOfAllowAndOmitsMenusWithoutAllow() throws Exception {
        createMenuPermission(AuthoritiesConstants.ADMIN, "security.users", "ALLOW");
        createMenuPermission(AuthoritiesConstants.USER, "security.users", "DENY");
        createMenuPermission(AuthoritiesConstants.USER, "entities.department", "DENY");
        createMenuPermission(AuthoritiesConstants.ADMIN, "entities.organization", "ALLOW");

        MvcResult result = restMockMvc
            .perform(
                get(ENTITY_API_URL)
                    .param("appName", APP_NAME)
                    .with(
                        user("menu-user").authorities(
                            new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN),
                            new SimpleGrantedAuthority(AuthoritiesConstants.USER)
                        )
                    )
            )
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        List<String> allowedMenuIds = arrayValues(body.path("allowedMenuIds"));

        assertThat(body.path("appName").asText()).isEqualTo(APP_NAME);
        assertThat(body.path("allowedMenuIds").isArray()).isTrue();
        assertThat(body.path("allowedMenuIds").size()).isEqualTo(2);
        assertThat(allowedMenuIds).containsExactly("entities.organization", "security.users");
        assertThat(allowedMenuIds).doesNotContain("entities.department");
        assertThat(fieldNames(body)).containsExactlyInAnyOrder("appName", "allowedMenuIds");
    }

    @Test
    void getMenuPermissions_keepsCurrentUserResultsIsolatedByAppName() throws Exception {
        createMenuPermission(AuthoritiesConstants.ADMIN, APP_NAME, "entities.organization", "ALLOW");
        createMenuPermission(AuthoritiesConstants.ADMIN, APP_NAME, "security.users", "ALLOW");
        createMenuPermission(AuthoritiesConstants.ADMIN, "other-admin-app", "ops.dashboard", "ALLOW");

        MvcResult result = restMockMvc
            .perform(
                get(ENTITY_API_URL + "?appName=jhipster-security-platform").with(
                    user("menu-user").authorities(new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN))
                )
            )
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        List<String> allowedMenuIds = arrayValues(body.path("allowedMenuIds"));

        assertThat(body.path("appName").asText()).isEqualTo(APP_NAME);
        assertThat(allowedMenuIds).containsExactly("entities.organization", "security.users");
        assertThat(allowedMenuIds).doesNotContain("ops.dashboard");
    }

    @Test
    void getMenuPermissions_whenUnauthenticated_returnsUnauthorized() throws Exception {
        restMockMvc.perform(get(ENTITY_API_URL).param("appName", APP_NAME)).andExpect(status().isUnauthorized());
    }

    private void createMenuPermission(String role, String menuId, String effect) throws Exception {
        createMenuPermission(role, APP_NAME, menuId, effect);
    }

    private void createMenuPermission(String role, String appName, String menuId, String effect) throws Exception {
        SecMenuPermissionDTO dto = new SecMenuPermissionDTO();
        dto.setRole(role);
        dto.setAppName(appName);
        dto.setMenuId(menuId);
        dto.setEffect(effect);

        restMockMvc
            .perform(
                post(ADMIN_ENTITY_API_URL)
                    .with(user("admin").authorities(new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(dto))
            )
            .andExpect(status().isCreated());
    }

    private List<String> arrayValues(JsonNode arrayNode) {
        List<String> values = new ArrayList<>();
        arrayNode.elements().forEachRemaining(node -> values.add(node.asText()));
        return values;
    }

    private List<String> fieldNames(JsonNode body) {
        Iterator<String> fieldNames = body.fieldNames();
        List<String> names = new ArrayList<>();
        fieldNames.forEachRemaining(names::add);
        return names;
    }
}
