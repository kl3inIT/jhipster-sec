package com.vn.core.web.rest.admin.security;

import com.vn.core.domain.Authority;
import com.vn.core.domain.RoleType;
import com.vn.core.repository.AuthorityRepository;
import com.vn.core.repository.UserRepository;
import com.vn.core.security.AuthoritiesConstants;
import com.vn.core.security.repository.SecPermissionRepository;
import com.vn.core.service.dto.security.SecRoleDTO;
import com.vn.core.service.security.SecPermissionService;
import com.vn.core.web.rest.errors.BadRequestAlertException;
import com.vn.core.web.rest.errors.RoleInUseException;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing security roles (Authority entities).
 * Exposes full CRUD at /api/admin/sec/roles for admin users only.
 */
@RestController
@RequestMapping("/api/admin/sec/roles")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
public class SecRoleAdminResource {

    private static final Logger LOG = LoggerFactory.getLogger(SecRoleAdminResource.class);

    private static final String ENTITY_NAME = "secRole";

    @Value("${jhipster.clientApp.name:jhipstersec}")
    private String applicationName;

    private final AuthorityRepository authorityRepository;

    private final SecPermissionRepository secPermissionRepository;

    private final UserRepository userRepository;

    private final SecPermissionService secPermissionService;

    public SecRoleAdminResource(
        AuthorityRepository authorityRepository,
        SecPermissionRepository secPermissionRepository,
        UserRepository userRepository,
        SecPermissionService secPermissionService
    ) {
        this.authorityRepository = authorityRepository;
        this.secPermissionRepository = secPermissionRepository;
        this.userRepository = userRepository;
        this.secPermissionService = secPermissionService;
    }

    /**
     * {@code POST /api/admin/sec/roles} : Create a new security role.
     *
     * @param dto the role to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and the new role, or {@code 400 (Bad Request)} if the name already exists.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<SecRoleDTO> createRole(@Valid @RequestBody SecRoleDTO dto) throws URISyntaxException {
        LOG.debug("REST request to create SecRole : {}", dto);
        if (authorityRepository.findById(dto.getName()).isPresent()) {
            throw new BadRequestAlertException("A role with this name already exists", ENTITY_NAME, "nameexists");
        }
        Authority authority = new Authority().name(dto.getName()).displayName(dto.getDisplayName()).type(RoleType.valueOf(dto.getType()));
        authorityRepository.save(authority);
        SecRoleDTO resultDto = toDto(authority);
        return ResponseEntity.created(new URI("/api/admin/sec/roles/" + dto.getName()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, dto.getName()))
            .body(resultDto);
    }

    /**
     * {@code GET /api/admin/sec/roles} : Get all security roles.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of roles.
     */
    @GetMapping("")
    public ResponseEntity<List<SecRoleDTO>> getAllRoles() {
        LOG.debug("REST request to get all SecRoles");
        List<SecRoleDTO> roles = authorityRepository.findAll().stream().map(this::toDto).toList();
        return ResponseEntity.ok(roles);
    }

    /**
     * {@code GET /api/admin/sec/roles/{name}} : Get a security role by name.
     *
     * @param name the name of the role to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the role, or {@code 404 (Not Found)}.
     */
    @GetMapping("/{name}")
    public ResponseEntity<SecRoleDTO> getRole(@PathVariable("name") String name) {
        LOG.debug("REST request to get SecRole : {}", name);
        return ResponseUtil.wrapOrNotFound(authorityRepository.findById(name).map(this::toDto));
    }

    /**
     * {@code PUT /api/admin/sec/roles/{name}} : Update an existing security role.
     *
     * @param name the name in the URL path.
     * @param dto the role data to apply.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the updated role, or {@code 400} if invalid.
     */
    @PutMapping("/{name}")
    public ResponseEntity<SecRoleDTO> updateRole(@PathVariable("name") String name, @Valid @RequestBody SecRoleDTO dto) {
        LOG.debug("REST request to update SecRole : {}", name);
        if (!name.equals(dto.getName())) {
            throw new BadRequestAlertException("Name in path and body must match", ENTITY_NAME, "namemismatch");
        }
        Authority authority = authorityRepository
            .findById(name)
            .orElseThrow(() -> new BadRequestAlertException("Role not found", ENTITY_NAME, "notfound"));
        authority.setDisplayName(dto.getDisplayName());
        authority.setType(RoleType.valueOf(dto.getType()));
        authorityRepository.save(authority);
        return ResponseEntity.ok(toDto(authority));
    }

    /**
     * {@code DELETE /api/admin/sec/roles/{name}} : Delete a security role and cascade-delete its permissions.
     *
     * @param name the name of the role to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (No Content)}.
     */
    @DeleteMapping("/{name}")
    @Transactional
    public ResponseEntity<Void> deleteRole(@PathVariable("name") String name) {
        LOG.debug("REST request to delete SecRole : {}", name);
        long assignedUserCount = userRepository.countByAuthorityName(name);
        if (assignedUserCount > 0) {
            throw new RoleInUseException();
        }
        secPermissionService.deleteAllByAuthorityName(name);
        authorityRepository.deleteById(name);
        return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, name)).build();
    }

    private SecRoleDTO toDto(Authority authority) {
        SecRoleDTO dto = new SecRoleDTO();
        dto.setName(authority.getName());
        dto.setDisplayName(authority.getDisplayName());
        dto.setType(authority.getType().name());
        return dto;
    }
}
