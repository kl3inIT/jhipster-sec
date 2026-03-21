package com.vn.core.web.rest.admin.security;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vn.core.IntegrationTest;
import com.vn.core.security.AuthoritiesConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for the {@link SecCatalogAdminResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class SecCatalogAdminResourceIT {

    private static final String CATALOG_API_URL = "/api/admin/sec/catalog";

    @Autowired
    private MockMvc restMockMvc;

    @Test
    void getCatalog_returnsAllSecuredEntities() throws Exception {
        restMockMvc
            .perform(get(CATALOG_API_URL))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))))
            .andExpect(jsonPath("$[*].code").isArray())
            .andExpect(jsonPath("$[*].displayName").isArray())
            .andExpect(jsonPath("$[0].operations").isArray())
            .andExpect(jsonPath("$[0].attributes").isArray());
    }

    @Test
    void getCatalog_organizationHasExpectedAttributes() throws Exception {
        restMockMvc
            .perform(get(CATALOG_API_URL))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(
                jsonPath("$[?(@.code == 'organization')].attributes[*]").value(
                    containsInAnyOrder("budget", "code", "departments", "id", "name", "ownerLogin")
                )
            );
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void getCatalog_requiresAdminAuthority() throws Exception {
        restMockMvc.perform(get(CATALOG_API_URL)).andExpect(status().isForbidden());
    }
}
