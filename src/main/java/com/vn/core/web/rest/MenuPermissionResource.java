package com.vn.core.web.rest;

import com.vn.core.security.domain.MenuAppName;
import com.vn.core.service.dto.security.MenuPermissionResponseDTO;
import com.vn.core.service.security.CurrentUserMenuPermissionService;
import com.vn.core.web.rest.errors.BadRequestAlertException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Security", description = "Current-user security metadata endpoints.")
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
    @Operation(
        operationId = "getMenuPermissions",
        summary = "Get current-user menu permissions for an app",
        description = "Returns the list of menu IDs the current user is allowed to see for the specified app. Used " +
        "by the frontend navigation service to filter the menu tree."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Invalid appName parameter", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
    })
    @GetMapping("/menu-permissions")
    public ResponseEntity<MenuPermissionResponseDTO> getMenuPermissions(
        @Parameter(description = "Frontend app identifier (e.g., 'MAIN')", required = true) @RequestParam("appName") String appName
    ) {
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
