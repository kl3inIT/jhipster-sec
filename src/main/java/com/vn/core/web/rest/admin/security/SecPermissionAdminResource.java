package com.vn.core.web.rest.admin.security;

import com.vn.core.repository.AuthorityRepository;
import com.vn.core.security.AuthoritiesConstants;
import com.vn.core.security.domain.SecPermission;
import com.vn.core.security.permission.TargetType;
import com.vn.core.security.repository.SecPermissionRepository;
import com.vn.core.service.dto.security.SecPermissionDTO;
import com.vn.core.service.mapper.security.SecPermissionMapper;
import com.vn.core.service.security.SecPermissionService;
import com.vn.core.service.security.SecPermissionUiContractService;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing security permissions.
 * Exposes full CRUD at /api/admin/sec/permissions for admin users only.
 *
 * <p>This controller is HTTP transport only. All persistence, duplicate cleanup, and
 * permission-cache eviction are delegated to {@link SecPermissionService}, consistent with
 * CLAUDE.md layering rules.
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

    private final SecPermissionUiContractService secPermissionUiContractService;

    private final SecPermissionService secPermissionService;

    public SecPermissionAdminResource(
        SecPermissionRepository secPermissionRepository,
        SecPermissionMapper secPermissionMapper,
        AuthorityRepository authorityRepository,
        SecPermissionUiContractService secPermissionUiContractService,
        SecPermissionService secPermissionService
    ) {
        this.secPermissionRepository = secPermissionRepository;
        this.secPermissionMapper = secPermissionMapper;
        this.authorityRepository = authorityRepository;
        this.secPermissionUiContractService = secPermissionUiContractService;
        this.secPermissionService = secPermissionService;
    }

    /**
     * {@code POST /api/admin/sec/permissions} : Create a new permission.
     *
     * @param dto the permission to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and the new permission, or {@code 400} if id is set or role not found.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    @Transactional
    public ResponseEntity<SecPermissionDTO> createPermission(@Valid @RequestBody SecPermissionDTO dto) throws URISyntaxException {
        LOG.debug("REST request to create SecPermission : {}", dto);
        if (dto.getId() != null) {
            throw new BadRequestAlertException("A new permission cannot already have an ID", ENTITY_NAME, "idexists");
        }
        if (authorityRepository.findById(dto.getAuthorityName()).isEmpty()) {
            throw new BadRequestAlertException("The specified role does not exist", ENTITY_NAME, "rolenotfound");
        }
        SecPermissionDTO normalizedDto = secPermissionUiContractService.normalizeIncoming(dto);
        List<SecPermission> existingPermissions = secPermissionService.findDuplicates(
            normalizedDto.getAuthorityName(),
            TargetType.valueOf(normalizedDto.getTargetType()),
            normalizedDto.getTarget(),
            normalizedDto.getAction()
        );
        if (!existingPermissions.isEmpty()) {
            SecPermission canonicalPermission = existingPermissions
                .stream()
                .filter(permission -> normalizedDto.getEffect().equals(permission.getEffect()))
                .findFirst()
                .orElse(existingPermissions.getFirst());
            if (!normalizedDto.getEffect().equals(canonicalPermission.getEffect())) {
                canonicalPermission.setEffect(normalizedDto.getEffect());
                canonicalPermission = secPermissionService.update(canonicalPermission);
            }
            Long canonicalPermissionId = canonicalPermission.getId();
            List<SecPermission> duplicatesToDelete = existingPermissions
                .stream()
                .filter(permission -> !permission.getId().equals(canonicalPermissionId))
                .toList();
            if (!duplicatesToDelete.isEmpty()) {
                secPermissionService.deleteAll(duplicatesToDelete);
            }
            deleteRedundantSpecificPermissions(normalizedDto);
            return ResponseEntity.ok(secPermissionUiContractService.normalizeOutgoing(secPermissionMapper.toDto(canonicalPermission)));
        }
        var entity = secPermissionMapper.toEntity(normalizedDto);
        entity = secPermissionService.save(entity);
        deleteRedundantSpecificPermissions(normalizedDto);
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
        return ResponseEntity.ok(permissions.stream().map(secPermissionUiContractService::normalizeOutgoing).toList());
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
        Optional<SecPermissionDTO> dto = secPermissionService
            .findById(id)
            .map(secPermissionMapper::toDto)
            .map(secPermissionUiContractService::normalizeOutgoing);
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
        if (secPermissionService.findById(id).isEmpty()) {
            throw new BadRequestAlertException("Permission not found", ENTITY_NAME, "notfound");
        }
        if (authorityRepository.findById(dto.getAuthorityName()).isEmpty()) {
            throw new BadRequestAlertException("The specified role does not exist", ENTITY_NAME, "rolenotfound");
        }
        SecPermissionDTO normalizedDto = secPermissionUiContractService.normalizeIncoming(dto);
        var entity = secPermissionMapper.toEntity(normalizedDto);
        entity = secPermissionService.update(entity);
        return ResponseEntity.ok(secPermissionMapper.toDto(entity));
    }

    /**
     * Deletes redundant specific permissions when a wildcard is saved.
     * <ul>
     *   <li>Entity wildcard ({@code target='*'}): removes all specific entity rows for same authority+action+effect.</li>
     *   <li>Attribute wildcard ({@code target='ENTITY.*'}): removes all specific attribute rows under the same entity prefix.</li>
     * </ul>
     */
    private void deleteRedundantSpecificPermissions(SecPermissionDTO normalizedDto) {
        String targetType = normalizedDto.getTargetType();
        String target = normalizedDto.getTarget();
        String authority = normalizedDto.getAuthorityName();
        String action = normalizedDto.getAction();
        String effect = normalizedDto.getEffect();

        if (TargetType.ENTITY.name().equals(targetType) && "*".equals(target)) {
            secPermissionRepository.deleteSpecificEntityPermissions(authority, action, effect);
        } else if (TargetType.ATTRIBUTE.name().equals(targetType) && target != null && target.endsWith(".*")) {
            String entityCode = target.substring(0, target.length() - 2);
            secPermissionRepository.deleteSpecificAttributePermissions(authority, entityCode + ".%", target, action, effect);
        }
    }

    /**
     * {@code DELETE /api/admin/sec/permissions/{id}} : Delete a permission.
     *
     * @param id the id of the permission to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (No Content)}.
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deletePermission(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete SecPermission : {}", id);
        secPermissionService.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
