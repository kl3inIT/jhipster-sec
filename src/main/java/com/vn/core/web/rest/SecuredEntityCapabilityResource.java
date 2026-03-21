package com.vn.core.web.rest;

import com.vn.core.service.dto.security.SecuredEntityCapabilityDTO;
import com.vn.core.service.security.SecuredEntityCapabilityService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing current-user secured entity capabilities.
 */
@RestController
@RequestMapping("/api/security")
@PreAuthorize("isAuthenticated()")
public class SecuredEntityCapabilityResource {

    private static final Logger LOG = LoggerFactory.getLogger(SecuredEntityCapabilityResource.class);

    private final SecuredEntityCapabilityService securedEntityCapabilityService;

    public SecuredEntityCapabilityResource(SecuredEntityCapabilityService securedEntityCapabilityService) {
        this.securedEntityCapabilityService = securedEntityCapabilityService;
    }

    /**
     * {@code GET /api/security/entity-capabilities} : Get current-user entity and attribute capabilities.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the capability list in the body.
     */
    @GetMapping("/entity-capabilities")
    public ResponseEntity<List<SecuredEntityCapabilityDTO>> getEntityCapabilities() {
        LOG.debug("REST request to get current-user secured entity capabilities");
        return ResponseEntity.ok(securedEntityCapabilityService.getCurrentUserCapabilities());
    }
}
