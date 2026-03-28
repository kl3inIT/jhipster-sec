package com.vn.core.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.core.IntegrationTest;
import com.vn.core.config.Constants;
import com.vn.core.domain.Authority;
import com.vn.core.domain.User;
import com.vn.core.repository.AuthorityRepository;
import com.vn.core.repository.UserRepository;
import com.vn.core.service.UserService;
import com.vn.core.service.dto.AdminUserDTO;
import com.vn.core.web.rest.vm.LoginVM;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    @Test
    void sameTokenReflectsUpdatedAuthorities() throws Exception {
        User user = saveActivatedUser("capability-refresh", "capability-refresh@example.com", "password", "ROLE_PROOF_READER");
        String token = authenticate(user.getLogin(), "password");

        JsonNode readerOrganization = getCapability("organization", token);
        assertThat(readerOrganization.path("canRead").asBoolean()).isTrue();
        assertThat(readerOrganization.path("canUpdate").asBoolean()).isFalse();

        updateAuthorities(user.getId(), user.getLogin(), "ROLE_PROOF_EDITOR");

        JsonNode editorOrganization = getCapability("organization", token);
        assertThat(editorOrganization.path("canRead").asBoolean()).isTrue();
        assertThat(editorOrganization.path("canUpdate").asBoolean()).isTrue();
        assertThat(attribute(editorOrganization, "budget").path("canEdit").asBoolean()).isFalse();
    }

    private JsonNode getCapability(String code) throws Exception {
        return getCapability(code, null);
    }

    private JsonNode getCapability(String code, String token) throws Exception {
        var requestBuilder = get(ENTITY_CAPABILITIES_API_URL);
        if (token != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, bearerToken(token));
        }
        MvcResult result = restMockMvc
            .perform(requestBuilder)
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

    private User saveActivatedUser(String login, String email, String rawPassword, String... authorityNames) {
        User user = new User();
        user.setLogin(login);
        user.setEmail(email);
        user.setActivated(true);
        user.setLangKey(Constants.DEFAULT_LANGUAGE);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setAuthorities(resolveAuthorities(authorityNames));
        return userRepository.saveAndFlush(user);
    }

    private Set<Authority> resolveAuthorities(String... authorityNames) {
        Set<Authority> authorities = new HashSet<>();
        for (String authorityName : authorityNames) {
            authorities.add(authorityRepository.findById(authorityName).orElseThrow());
        }
        return authorities;
    }

    private void updateAuthorities(Long userId, String login, String... authorityNames) {
        AdminUserDTO updatedUser = new AdminUserDTO(userRepository.findOneWithAuthoritiesByLogin(login).orElseThrow());
        updatedUser.setId(userId);
        updatedUser.setAuthorities(Set.of(authorityNames));
        userService.updateUser(updatedUser);
    }

    private String authenticate(String username, String password) throws Exception {
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername(username);
        loginVM.setPassword(password);

        MvcResult result = restMockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(loginVM)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("id_token").asText();
    }

    private String bearerToken(String token) {
        return "Bearer " + token;
    }
}
