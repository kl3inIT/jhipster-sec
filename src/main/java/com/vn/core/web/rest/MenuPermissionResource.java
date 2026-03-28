package com.vn.core.web.rest;

import com.vn.core.security.domain.MenuAppName;
import com.vn.core.service.dto.security.MenuPermissionResponseDTO;
import com.vn.core.service.security.CurrentUserMenuPermissionService;
import com.vn.core.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing app-scoped menu permissions for the current user.
 */
@RestController
@RequestMapping("/api/security")
@PreAuthorize("isAuthenticated()")
public class MenuPermissionResource {

    private static final Logger LOG = LoggerFactory.getLogger(MenuPermissionResource.class);
    private static final String ENTITY_NAME = "menuPermission";

    private final CurrentUserMenuPermissionService currentUserMenuPermissionService;

    public MenuPermissionResource(CurrentUserMenuPermissionService currentUserMenuPermissionService) {
        this.currentUserMenuPermissionService = currentUserMenuPermissionService;
    }

    /**
     * {@code GET /api/security/menu-permissions} : Get current-user allowed menu ids for one app.
     *
     * @param appName the frontend app identifier.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the menu permission payload in the body.
     */
    @GetMapping("/menu-permissions")
    public ResponseEntity<MenuPermissionResponseDTO> getMenuPermissions(@RequestParam("appName") String appName) {
        LOG.debug("REST request to get current-user menu permissions for app {}", appName);
        MenuAppName menuAppName = parseAppName(appName);
        MenuPermissionResponseDTO response = new MenuPermissionResponseDTO();
        response.setAppName(menuAppName.getValue());
        response.setAllowedMenuIds(currentUserMenuPermissionService.getAllowedMenuIds(menuAppName));
        return ResponseEntity.ok(response);
    }

    private MenuAppName parseAppName(String appName) {
        return MenuAppName.fromValue(appName).orElseThrow(() ->
            new BadRequestAlertException("Invalid menu app name", ENTITY_NAME, "appNameInvalid")
        );
    }
}
