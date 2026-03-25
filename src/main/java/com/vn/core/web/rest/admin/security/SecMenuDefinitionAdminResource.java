package com.vn.core.web.rest.admin.security;

import com.vn.core.security.AuthoritiesConstants;
import com.vn.core.security.domain.SecMenuDefinition;
import com.vn.core.security.repository.SecMenuDefinitionRepository;
import com.vn.core.security.repository.SecMenuPermissionRepository;
import com.vn.core.service.dto.security.SecMenuDefinitionDTO;
import com.vn.core.service.dto.security.SyncNodeDTO;
import com.vn.core.service.dto.security.SyncResultDTO;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing menu definitions.
 * Exposes full CRUD plus a sync endpoint at /api/admin/sec/menu-definitions for admin users only.
 */
@RestController
@RequestMapping("/api/admin/sec/menu-definitions")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
public class SecMenuDefinitionAdminResource {

    private static final Logger LOG = LoggerFactory.getLogger(SecMenuDefinitionAdminResource.class);

    private static final String ENTITY_NAME = "secMenuDefinition";

    @Value("${jhipster.clientApp.name:jhipstersec}")
    private String applicationName;

    private final SecMenuDefinitionRepository secMenuDefinitionRepository;

    private final SecMenuPermissionRepository secMenuPermissionRepository;

    public SecMenuDefinitionAdminResource(
        SecMenuDefinitionRepository secMenuDefinitionRepository,
        SecMenuPermissionRepository secMenuPermissionRepository
    ) {
        this.secMenuDefinitionRepository = secMenuDefinitionRepository;
        this.secMenuPermissionRepository = secMenuPermissionRepository;
    }

    /**
     * {@code POST /api/admin/sec/menu-definitions} : Create a new menu definition.
     *
     * @param dto the menu definition to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and the new definition, or {@code 400} if menuId already exists for the app.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<SecMenuDefinitionDTO> createMenuDefinition(@Valid @RequestBody SecMenuDefinitionDTO dto)
        throws URISyntaxException {
        LOG.debug("REST request to create SecMenuDefinition : {}", dto);
        if (secMenuDefinitionRepository.findByAppNameAndMenuId(dto.getAppName(), dto.getMenuId()).isPresent()) {
            throw new BadRequestAlertException(
                "A menu definition with this menuId already exists for this app",
                ENTITY_NAME,
                "menuIdExists"
            );
        }
        SecMenuDefinition entity = toEntity(dto);
        entity = secMenuDefinitionRepository.save(entity);
        SecMenuDefinitionDTO result = toDto(entity);
        return ResponseEntity.created(new URI("/api/admin/sec/menu-definitions/" + entity.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, dto.getMenuId()))
            .body(result);
    }

    /**
     * {@code GET /api/admin/sec/menu-definitions} : Get all menu definitions for an app.
     *
     * @param appName the application name to filter by.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of definitions.
     */
    @GetMapping("")
    public ResponseEntity<List<SecMenuDefinitionDTO>> getAllMenuDefinitions(
        @RequestParam(required = false, defaultValue = "jhipster-security-platform") String appName
    ) {
        LOG.debug("REST request to get all SecMenuDefinitions for appName={}", appName);
        List<SecMenuDefinitionDTO> dtos = secMenuDefinitionRepository.findAllByAppName(appName).stream().map(this::toDto).toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * {@code GET /api/admin/sec/menu-definitions/{id}} : Get a menu definition by id.
     *
     * @param id the id of the definition to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the definition, or {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SecMenuDefinitionDTO> getMenuDefinition(@PathVariable("id") Long id) {
        LOG.debug("REST request to get SecMenuDefinition : {}", id);
        return ResponseUtil.wrapOrNotFound(secMenuDefinitionRepository.findById(id).map(this::toDto));
    }

    /**
     * {@code PUT /api/admin/sec/menu-definitions/{id}} : Update an existing menu definition.
     *
     * @param id the id in the path.
     * @param dto the menu definition data to apply.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the updated definition, or {@code 400} if invalid.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SecMenuDefinitionDTO> updateMenuDefinition(
        @PathVariable("id") Long id,
        @Valid @RequestBody SecMenuDefinitionDTO dto
    ) {
        LOG.debug("REST request to update SecMenuDefinition : {}", id);
        if (dto.getId() == null || !id.equals(dto.getId())) {
            throw new BadRequestAlertException("ID in path and body must match", ENTITY_NAME, "idMismatch");
        }
        SecMenuDefinition existing = secMenuDefinitionRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Menu definition not found", ENTITY_NAME, "notfound"));
        existing.setMenuName(dto.getMenuName());
        existing.setLabel(dto.getLabel());
        existing.setDescription(dto.getDescription());
        existing.setParentMenuId(dto.getParentMenuId());
        existing.setRoute(dto.getRoute());
        existing.setIcon(dto.getIcon());
        existing.setOrdering(dto.getOrdering());
        existing = secMenuDefinitionRepository.save(existing);
        return ResponseEntity.ok(toDto(existing));
    }

    /**
     * {@code DELETE /api/admin/sec/menu-definitions/{id}} : Delete a menu definition and cascade-delete its permissions.
     *
     * @param id the id of the definition to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (No Content)}.
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteMenuDefinition(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete SecMenuDefinition : {}", id);
        SecMenuDefinition entity = secMenuDefinitionRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Menu definition not found", ENTITY_NAME, "notfound"));
        secMenuPermissionRepository.deleteByAppNameAndMenuId(entity.getAppName(), entity.getMenuId());
        secMenuDefinitionRepository.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, entity.getMenuId()))
            .build();
    }

    /**
     * {@code POST /api/admin/sec/menu-definitions/sync} : Sync menu definitions from a registry.
     * Inserts only nodes that do not already exist (by appName + menuId). Skips existing rows.
     *
     * @param nodes the list of nodes to sync.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and a sync result summary.
     */
    @PostMapping("/sync")
    @Transactional
    public ResponseEntity<SyncResultDTO> syncFromRegistry(@Valid @RequestBody List<SyncNodeDTO> nodes) {
        LOG.debug("REST request to sync {} menu definition nodes", nodes.size());
        int seeded = 0;
        int skipped = 0;
        for (SyncNodeDTO node : nodes) {
            if (secMenuDefinitionRepository.findByAppNameAndMenuId(node.getAppName(), node.getMenuId()).isPresent()) {
                skipped++;
            } else {
                SecMenuDefinition entity = new SecMenuDefinition()
                    .menuId(node.getMenuId())
                    .appName(node.getAppName())
                    .menuName(node.getMenuName())
                    .label(node.getLabel())
                    .description(null)
                    .parentMenuId(node.getParentMenuId())
                    .route(node.getRoute())
                    .icon(node.getIcon())
                    .ordering(node.getOrdering());
                secMenuDefinitionRepository.save(entity);
                seeded++;
            }
        }
        return ResponseEntity.ok(new SyncResultDTO(seeded, skipped));
    }

    private SecMenuDefinitionDTO toDto(SecMenuDefinition entity) {
        SecMenuDefinitionDTO dto = new SecMenuDefinitionDTO();
        dto.setId(entity.getId());
        dto.setMenuId(entity.getMenuId());
        dto.setAppName(entity.getAppName());
        dto.setMenuName(entity.getMenuName());
        dto.setLabel(entity.getLabel());
        dto.setDescription(entity.getDescription());
        dto.setParentMenuId(entity.getParentMenuId());
        dto.setRoute(entity.getRoute());
        dto.setIcon(entity.getIcon());
        dto.setOrdering(entity.getOrdering());
        return dto;
    }

    private SecMenuDefinition toEntity(SecMenuDefinitionDTO dto) {
        return new SecMenuDefinition()
            .menuId(dto.getMenuId())
            .appName(dto.getAppName())
            .menuName(dto.getMenuName())
            .label(dto.getLabel())
            .description(dto.getDescription())
            .parentMenuId(dto.getParentMenuId())
            .route(dto.getRoute())
            .icon(dto.getIcon())
            .ordering(dto.getOrdering());
    }
}
