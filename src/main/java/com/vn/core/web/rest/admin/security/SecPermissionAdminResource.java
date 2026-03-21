package com.vn.core.web.rest.admin.security;

import com.vn.core.repository.AuthorityRepository;
import com.vn.core.security.AuthoritiesConstants;
import com.vn.core.security.repository.SecPermissionRepository;
import com.vn.core.service.dto.security.SecPermissionDTO;
import com.vn.core.service.mapper.security.SecPermissionMapper;
import com.vn.core.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing security permissions.
 * Exposes full CRUD at /api/admin/sec/permissions for admin users only.
 */
@RestController
@RequestMapping("/api/admin/sec/permissions")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
public class SecPermissionAdminResource {

    private static final Logger LOG = LoggerFactory.getLogger(SecPermissionAdminResource.class);

    private static final String ENTITY_NAME = "secPermission";

    @Value("${jhipster.clientApp.name:jhipstersec}")
    private String applicationName;

    private final SecPermissionRepository secPermissionRepository;

    private final SecPermissionMapper secPermissionMapper;

    private final AuthorityRepository authorityRepository;

    public SecPermissionAdminResource(
        SecPermissionRepository secPermissionRepository,
        SecPermissionMapper secPermissionMapper,
        AuthorityRepository authorityRepository
    ) {
        this.secPermissionRepository = secPermissionRepository;
        this.secPermissionMapper = secPermissionMapper;
        this.authorityRepository = authorityRepository;
    }

    /**
     * {@code POST /api/admin/sec/permissions} : Create a new permission.
     *
     * @param dto the permission to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and the new permission, or {@code 400} if id is set or role not found.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<SecPermissionDTO> createPermission(@Valid @RequestBody SecPermissionDTO dto) throws URISyntaxException {
        LOG.debug("REST request to create SecPermission : {}", dto);
        if (dto.getId() != null) {
            throw new BadRequestAlertException("A new permission cannot already have an ID", ENTITY_NAME, "idexists");
        }
        if (authorityRepository.findById(dto.getAuthorityName()).isEmpty()) {
            throw new BadRequestAlertException("The specified role does not exist", ENTITY_NAME, "rolenotfound");
        }
        var entity = secPermissionMapper.toEntity(dto);
        entity = secPermissionRepository.save(entity);
        SecPermissionDTO result = secPermissionMapper.toDto(entity);
        return ResponseEntity.created(new URI("/api/admin/sec/permissions/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET /api/admin/sec/permissions} : Get all permissions, optionally filtered by authority name.
     *
     * @param authorityName optional role name to filter by (e.g. "ROLE_ADMIN").
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of permissions.
     */
    @GetMapping("")
    public ResponseEntity<List<SecPermissionDTO>> getAllPermissions(@RequestParam(required = false) String authorityName) {
        LOG.debug("REST request to get all SecPermissions, authorityName={}", authorityName);
        List<SecPermissionDTO> permissions;
        if (authorityName != null && !authorityName.isBlank()) {
            permissions = secPermissionMapper.toDto(secPermissionRepository.findByAuthorityName(authorityName));
        } else {
            permissions = secPermissionMapper.toDto(secPermissionRepository.findAll());
        }
        return ResponseEntity.ok(permissions);
    }

    /**
     * {@code GET /api/admin/sec/permissions/{id}} : Get a permission by id.
     *
     * @param id the id of the permission to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the permission, or {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SecPermissionDTO> getPermission(@PathVariable("id") Long id) {
        LOG.debug("REST request to get SecPermission : {}", id);
        Optional<SecPermissionDTO> dto = secPermissionRepository.findById(id).map(secPermissionMapper::toDto);
        return ResponseUtil.wrapOrNotFound(dto);
    }

    /**
     * {@code PUT /api/admin/sec/permissions/{id}} : Update an existing permission.
     *
     * @param id the id in the path.
     * @param dto the permission data to apply.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the updated permission, or {@code 400} if invalid.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SecPermissionDTO> updatePermission(@PathVariable("id") Long id, @Valid @RequestBody SecPermissionDTO dto) {
        LOG.debug("REST request to update SecPermission : {}", id);
        if (dto.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!id.equals(dto.getId())) {
            throw new BadRequestAlertException("ID in path and body must match", ENTITY_NAME, "idmismatch");
        }
        if (secPermissionRepository.findById(id).isEmpty()) {
            throw new BadRequestAlertException("Permission not found", ENTITY_NAME, "notfound");
        }
        if (authorityRepository.findById(dto.getAuthorityName()).isEmpty()) {
            throw new BadRequestAlertException("The specified role does not exist", ENTITY_NAME, "rolenotfound");
        }
        var entity = secPermissionMapper.toEntity(dto);
        entity = secPermissionRepository.save(entity);
        return ResponseEntity.ok(secPermissionMapper.toDto(entity));
    }

    /**
     * {@code DELETE /api/admin/sec/permissions/{id}} : Delete a permission.
     *
     * @param id the id of the permission to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (No Content)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePermission(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete SecPermission : {}", id);
        secPermissionRepository.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
