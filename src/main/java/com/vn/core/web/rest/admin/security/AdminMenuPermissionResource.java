package com.vn.core.web.rest.admin.security;

import com.vn.core.repository.AuthorityRepository;
import com.vn.core.security.AuthoritiesConstants;
import com.vn.core.security.domain.SecMenuPermission;
import com.vn.core.security.repository.SecMenuPermissionRepository;
import com.vn.core.service.dto.security.SecMenuPermissionDTO;
import com.vn.core.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;

/**
 * REST controller for managing menu permissions.
 * Exposes create, query, and delete at /api/admin/sec/menu-permissions for admin users only.
 */
@RestController
@RequestMapping("/api/admin/sec/menu-permissions")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
public class AdminMenuPermissionResource {

    private static final Logger LOG = LoggerFactory.getLogger(AdminMenuPermissionResource.class);

    private static final String ENTITY_NAME = "secMenuPermission";

    @Value("${jhipster.clientApp.name:jhipstersec}")
    private String applicationName;

    private final SecMenuPermissionRepository secMenuPermissionRepository;

    private final AuthorityRepository authorityRepository;

    public AdminMenuPermissionResource(SecMenuPermissionRepository secMenuPermissionRepository, AuthorityRepository authorityRepository) {
        this.secMenuPermissionRepository = secMenuPermissionRepository;
        this.authorityRepository = authorityRepository;
    }

    /**
     * {@code POST /api/admin/sec/menu-permissions} : Create a new menu permission.
     *
     * @param dto the menu permission to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and the new permission, or {@code 400} if role not found.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<SecMenuPermissionDTO> createMenuPermission(@Valid @RequestBody SecMenuPermissionDTO dto)
        throws URISyntaxException {
        LOG.debug("REST request to create SecMenuPermission : {}", dto);
        if (authorityRepository.findById(dto.getRole()).isEmpty()) {
            throw new BadRequestAlertException("Role not found", ENTITY_NAME, "roleNotFound");
        }
        String effect = (dto.getEffect() == null || dto.getEffect().isBlank()) ? "ALLOW" : dto.getEffect();
        SecMenuPermission entity = new SecMenuPermission()
            .role(dto.getRole())
            .appName(dto.getAppName())
            .menuId(dto.getMenuId())
            .effect(effect);
        entity = secMenuPermissionRepository.save(entity);
        SecMenuPermissionDTO result = toDto(entity);
        return ResponseEntity.created(new URI("/api/admin/sec/menu-permissions/" + entity.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, entity.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET /api/admin/sec/menu-permissions} : Query menu permissions by role.
     *
     * @param role the role to filter by.
     * @param appName the application name to filter by.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of permissions.
     */
    @GetMapping("")
    public ResponseEntity<List<SecMenuPermissionDTO>> getMenuPermissions(
        @RequestParam String role,
        @RequestParam(required = false, defaultValue = "jhipster-security-platform") String appName
    ) {
        LOG.debug("REST request to get SecMenuPermissions for role={}, appName={}", role, appName);
        List<SecMenuPermissionDTO> dtos = secMenuPermissionRepository
            .findAllByRole(role)
            .stream()
            .filter(p -> appName.equals(p.getAppName()))
            .map(this::toDto)
            .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * {@code DELETE /api/admin/sec/menu-permissions/{id}} : Delete a menu permission.
     *
     * @param id the id of the permission to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (No Content)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMenuPermission(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete SecMenuPermission : {}", id);
        secMenuPermissionRepository.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    private SecMenuPermissionDTO toDto(SecMenuPermission entity) {
        SecMenuPermissionDTO dto = new SecMenuPermissionDTO();
        dto.setId(entity.getId());
        dto.setRole(entity.getRole());
        dto.setAppName(entity.getAppName());
        dto.setMenuId(entity.getMenuId());
        dto.setEffect(entity.getEffect());
        return dto;
    }
}
