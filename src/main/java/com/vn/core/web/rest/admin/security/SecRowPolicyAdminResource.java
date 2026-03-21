package com.vn.core.web.rest.admin.security;

import com.vn.core.security.AuthoritiesConstants;
import com.vn.core.security.repository.SecRowPolicyRepository;
import com.vn.core.service.dto.security.SecRowPolicyDTO;
import com.vn.core.service.mapper.security.SecRowPolicyMapper;
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
 * REST controller for managing row-level security policies.
 * Exposes full CRUD at /api/admin/sec/row-policies for admin users only.
 */
@RestController
@RequestMapping("/api/admin/sec/row-policies")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
public class SecRowPolicyAdminResource {

    private static final Logger LOG = LoggerFactory.getLogger(SecRowPolicyAdminResource.class);

    private static final String ENTITY_NAME = "secRowPolicy";

    @Value("${jhipster.clientApp.name:jhipstersec}")
    private String applicationName;

    private final SecRowPolicyRepository secRowPolicyRepository;

    private final SecRowPolicyMapper secRowPolicyMapper;

    public SecRowPolicyAdminResource(SecRowPolicyRepository secRowPolicyRepository, SecRowPolicyMapper secRowPolicyMapper) {
        this.secRowPolicyRepository = secRowPolicyRepository;
        this.secRowPolicyMapper = secRowPolicyMapper;
    }

    /**
     * {@code POST /api/admin/sec/row-policies} : Create a new row policy.
     *
     * @param dto the row policy to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and the new row policy, or {@code 400} if id is set or code already exists.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<SecRowPolicyDTO> createRowPolicy(@Valid @RequestBody SecRowPolicyDTO dto) throws URISyntaxException {
        LOG.debug("REST request to create SecRowPolicy : {}", dto);
        if (dto.getId() != null) {
            throw new BadRequestAlertException("A new row policy cannot already have an ID", ENTITY_NAME, "idexists");
        }
        if (secRowPolicyRepository.findByCode(dto.getCode()).isPresent()) {
            throw new BadRequestAlertException("A row policy with this code already exists", ENTITY_NAME, "codeexists");
        }
        var entity = secRowPolicyMapper.toEntity(dto);
        entity = secRowPolicyRepository.save(entity);
        SecRowPolicyDTO result = secRowPolicyMapper.toDto(entity);
        return ResponseEntity.created(new URI("/api/admin/sec/row-policies/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET /api/admin/sec/row-policies} : Get all row policies.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of row policies.
     */
    @GetMapping("")
    public ResponseEntity<List<SecRowPolicyDTO>> getAllRowPolicies() {
        LOG.debug("REST request to get all SecRowPolicies");
        List<SecRowPolicyDTO> policies = secRowPolicyMapper.toDto(secRowPolicyRepository.findAll());
        return ResponseEntity.ok(policies);
    }

    /**
     * {@code GET /api/admin/sec/row-policies/{id}} : Get a row policy by id.
     *
     * @param id the id of the row policy to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the row policy, or {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SecRowPolicyDTO> getRowPolicy(@PathVariable("id") Long id) {
        LOG.debug("REST request to get SecRowPolicy : {}", id);
        Optional<SecRowPolicyDTO> dto = secRowPolicyRepository.findById(id).map(secRowPolicyMapper::toDto);
        return ResponseUtil.wrapOrNotFound(dto);
    }

    /**
     * {@code PUT /api/admin/sec/row-policies/{id}} : Update an existing row policy.
     *
     * @param id the id in the path.
     * @param dto the row policy data to apply.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the updated row policy, or {@code 400} if invalid.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SecRowPolicyDTO> updateRowPolicy(@PathVariable("id") Long id, @Valid @RequestBody SecRowPolicyDTO dto) {
        LOG.debug("REST request to update SecRowPolicy : {}", id);
        if (dto.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!id.equals(dto.getId())) {
            throw new BadRequestAlertException("ID in path and body must match", ENTITY_NAME, "idmismatch");
        }
        var existing = secRowPolicyRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Row policy not found", ENTITY_NAME, "notfound"));
        // Check code uniqueness only when the code is changing
        if (!existing.getCode().equals(dto.getCode())) {
            secRowPolicyRepository
                .findByCode(dto.getCode())
                .ifPresent(conflict -> {
                    if (!conflict.getId().equals(id)) {
                        throw new BadRequestAlertException("A row policy with this code already exists", ENTITY_NAME, "codeexists");
                    }
                });
        }
        var entity = secRowPolicyMapper.toEntity(dto);
        entity = secRowPolicyRepository.save(entity);
        return ResponseEntity.ok(secRowPolicyMapper.toDto(entity));
    }

    /**
     * {@code DELETE /api/admin/sec/row-policies/{id}} : Delete a row policy.
     *
     * @param id the id of the row policy to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (No Content)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRowPolicy(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete SecRowPolicy : {}", id);
        secRowPolicyRepository.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
