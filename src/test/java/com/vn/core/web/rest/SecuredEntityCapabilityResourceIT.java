package com.vn.core.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.core.IntegrationTest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link SecuredEntityCapabilityResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class SecuredEntityCapabilityResourceIT {

    private static final String ENTITY_CAPABILITIES_API_URL = "/api/security/entity-capabilities";

    @Autowired
    private MockMvc restMockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "proof-owner", authorities = "ROLE_PROOF_READER")
    void getEntityCapabilities_withReaderRole_returnsReadOnlyOrganizationAndDeniedBudgetView() throws Exception {
        JsonNode organization = getCapability("organization");

        assertThat(organization.path("canCreate").asBoolean()).isFalse();
        assertThat(organization.path("canRead").asBoolean()).isTrue();
        assertThat(organization.path("canUpdate").asBoolean()).isFalse();
        assertThat(organization.path("canDelete").asBoolean()).isFalse();
        assertThat(attribute(organization, "budget").path("canView").asBoolean()).isFalse();
    }

    @Test
    @WithMockUser(username = "proof-owner", authorities = "ROLE_PROOF_EDITOR")
    void getEntityCapabilities_withEditorRole_returnsOrganizationCrudAndDeniedBudgetEdit() throws Exception {
        JsonNode organization = getCapability("organization");

        assertThat(organization.path("canCreate").asBoolean()).isTrue();
        assertThat(organization.path("canRead").asBoolean()).isTrue();
        assertThat(organization.path("canUpdate").asBoolean()).isTrue();
        assertThat(organization.path("canDelete").asBoolean()).isTrue();
        assertThat(attribute(organization, "budget").path("canEdit").asBoolean()).isFalse();
    }

    @Test
    @WithMockUser(username = "proof-owner", authorities = "ROLE_PROOF_NONE")
    void getEntityCapabilities_withNoRole_returnsOrganizationWithNoCrudPermissions() throws Exception {
        JsonNode organization = getCapability("organization");

        assertThat(organization.path("canCreate").asBoolean()).isFalse();
        assertThat(organization.path("canRead").asBoolean()).isFalse();
        assertThat(organization.path("canUpdate").asBoolean()).isFalse();
        assertThat(organization.path("canDelete").asBoolean()).isFalse();
    }

    private JsonNode getCapability(String code) throws Exception {
        MvcResult result = restMockMvc
            .perform(get(ENTITY_CAPABILITIES_API_URL))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(body).isNotNull();
        assertThat(body.isArray()).isTrue();
        assertThat(body).anyMatch(node -> code.equals(node.path("code").asText()));
        assertThat(body).anyMatch(node -> "department".equals(node.path("code").asText()));
        assertThat(body).anyMatch(node -> "employee".equals(node.path("code").asText()));

        return findByCode(body, code);
    }

    private JsonNode findByCode(JsonNode capabilities, String code) {
        for (JsonNode capability : capabilities) {
            if (code.equals(capability.path("code").asText())) {
                return capability;
            }
        }
        throw new AssertionError("Missing capability for code " + code);
    }

    private JsonNode attribute(JsonNode capability, String name) {
        for (JsonNode attribute : capability.path("attributes")) {
            if (name.equals(attribute.path("name").asText())) {
                return attribute;
            }
        }
        throw new AssertionError("Missing attribute " + name + " in capability " + capability.path("code").asText());
    }
}
