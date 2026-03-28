package com.vn.core.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.core.IntegrationTest;
import com.vn.core.config.Constants;
import com.vn.core.domain.Authority;
import com.vn.core.domain.User;
import com.vn.core.repository.AuthorityRepository;
import com.vn.core.repository.UserRepository;
import com.vn.core.security.AuthoritiesConstants;
import com.vn.core.security.domain.MenuAppName;
import com.vn.core.service.UserService;
import com.vn.core.service.dto.security.SecMenuPermissionDTO;
import com.vn.core.web.rest.vm.LoginVM;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    private static final String APP_NAME = MenuAppName.JHIPSTER_SECURITY_PLATFORM.getValue();
    private static final String SALES_APP_NAME = MenuAppName.SALES_CONSOLE.getValue();
    private static final String ENTITY_API_URL = "/api/security/menu-permissions";
    private static final String ADMIN_ENTITY_API_URL = "/api/admin/sec/menu-permissions";

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
        createMenuPermission(AuthoritiesConstants.ADMIN, SALES_APP_NAME, "ops.dashboard", "ALLOW");

        MvcResult result = restMockMvc
            .perform(
                get(ENTITY_API_URL)
                    .param("appName", APP_NAME)
                    .with(user("menu-user").authorities(new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN)))
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
    void getMenuPermissions_withInvalidAppName_returnsBadRequest() throws Exception {
        restMockMvc
            .perform(
                get(ENTITY_API_URL)
                    .param("appName", "other-admin-app")
                    .with(user("menu-user").authorities(new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN)))
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void getMenuPermissions_whenUnauthenticated_returnsUnauthorized() throws Exception {
        restMockMvc.perform(get(ENTITY_API_URL).param("appName", APP_NAME)).andExpect(status().isUnauthorized());
    }

    @Test
    void sameTokenReflectsUpdatedAuthorities() throws Exception {
        createMenuPermission(AuthoritiesConstants.ADMIN, "security.users", "ALLOW");

        User user = saveActivatedUser("menu-refresh", "menu-refresh@example.com", "password", AuthoritiesConstants.USER);
        String token = authenticate(user.getLogin(), "password");

        restMockMvc
            .perform(get(ENTITY_API_URL).param("appName", APP_NAME).header(HttpHeaders.AUTHORIZATION, bearerToken(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.allowedMenuIds").isArray())
            .andExpect(jsonPath("$.allowedMenuIds").isEmpty());

        updateAuthorities(user.getId(), user.getLogin(), AuthoritiesConstants.ADMIN);

        restMockMvc
            .perform(get(ENTITY_API_URL).param("appName", APP_NAME).header(HttpHeaders.AUTHORIZATION, bearerToken(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.allowedMenuIds").isArray())
            .andExpect(jsonPath("$.allowedMenuIds[0]").value("security.users"));
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
        var updatedUser = new com.vn.core.service.dto.AdminUserDTO(userRepository.findOneWithAuthoritiesByLogin(login).orElseThrow());
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
            .andExpect(jsonPath("$.id_token").isString())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("id_token").asText();
    }

    private String bearerToken(String token) {
        return "Bearer " + token;
    }
}
