package com.vn.core.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.core.IntegrationTest;
import com.vn.core.service.security.CurrentUserNavigationGrantService;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Integration tests for the {@link NavigationGrantResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
class NavigationGrantResourceIT {

    private static final String APP_NAME = "jhipster-security-platform";
    private static final String ENTITY_API_URL = "/api/security/navigation-grants";

    @Autowired
    private MockMvc restMockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CurrentUserNavigationGrantService currentUserNavigationGrantService;

    @Test
    @WithMockUser(username = "nav-user", authorities = "ROLE_ADMIN")
    void getNavigationGrants_returnsAppNameAndAllowedNodeIds() throws Exception {
        when(currentUserNavigationGrantService.getAllowedNodeIds(APP_NAME)).thenReturn(List.of("entities.department", "security.users"));

        MvcResult result = restMockMvc
            .perform(get(ENTITY_API_URL).param("appName", APP_NAME))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(body.path("appName").asText()).isEqualTo(APP_NAME);
        assertThat(body.path("allowedNodeIds").isArray()).isTrue();
        assertThat(body.path("allowedNodeIds").size()).isEqualTo(2);
        assertThat(body.path("allowedNodeIds").get(0).asText()).isEqualTo("entities.department");
        assertThat(body.path("allowedNodeIds").get(1).asText()).isEqualTo("security.users");
        assertThat(fieldNames(body)).containsExactlyInAnyOrder("appName", "allowedNodeIds");
    }

    @Test
    void getNavigationGrants_whenUnauthenticated_returnsUnauthorized() throws Exception {
        restMockMvc.perform(get(ENTITY_API_URL).param("appName", APP_NAME)).andExpect(status().isUnauthorized());
    }

    private List<String> fieldNames(JsonNode body) {
        Iterator<String> fieldNames = body.fieldNames();
        List<String> names = new ArrayList<>();
        fieldNames.forEachRemaining(names::add);
        return names;
    }
}
