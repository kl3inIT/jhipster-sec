package com.vn.core.web.rest;

import com.vn.core.service.dto.security.NavigationGrantResponseDTO;
import com.vn.core.service.security.CurrentUserNavigationGrantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing app-scoped navigation grants for the current user.
 */
@RestController
@RequestMapping("/api/security")
@PreAuthorize("isAuthenticated()")
public class NavigationGrantResource {

    private static final Logger LOG = LoggerFactory.getLogger(NavigationGrantResource.class);

    private final CurrentUserNavigationGrantService currentUserNavigationGrantService;

    public NavigationGrantResource(CurrentUserNavigationGrantService currentUserNavigationGrantService) {
        this.currentUserNavigationGrantService = currentUserNavigationGrantService;
    }

    /**
     * {@code GET /api/security/navigation-grants} : Get current-user allowed shell node ids for one app.
     *
     * @param appName the frontend app identifier.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the navigation grant payload in the body.
     */
    @GetMapping("/navigation-grants")
    public ResponseEntity<NavigationGrantResponseDTO> getNavigationGrants(@RequestParam("appName") String appName) {
        LOG.debug("REST request to get current-user navigation grants for app {}", appName);
        NavigationGrantResponseDTO response = new NavigationGrantResponseDTO();
        response.setAppName(appName);
        response.setAllowedNodeIds(currentUserNavigationGrantService.getAllowedNodeIds(appName));
        return ResponseEntity.ok(response);
    }
}
