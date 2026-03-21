package com.vn.core.web.rest.admin.security;

import com.vn.core.security.AuthoritiesConstants;
import com.vn.core.security.catalog.SecuredEntityCatalog;
import com.vn.core.security.catalog.SecuredEntityEntry;
import com.vn.core.security.permission.EntityOp;
import com.vn.core.service.dto.security.SecCatalogEntryDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for exposing the secured entity catalog.
 * Returns all entities registered for security enforcement with their operations and JPA attributes.
 */
@RestController
@RequestMapping("/api/admin/sec/catalog")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
public class SecCatalogAdminResource {

    private static final Logger LOG = LoggerFactory.getLogger(SecCatalogAdminResource.class);

    private final SecuredEntityCatalog catalog;

    private final EntityManager entityManager;

    public SecCatalogAdminResource(SecuredEntityCatalog catalog, EntityManager entityManager) {
        this.catalog = catalog;
        this.entityManager = entityManager;
    }

    /**
     * {@code GET /api/admin/sec/catalog} : Get all secured entities with their operations and attributes.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the catalog entries.
     */
    @GetMapping("")
    public ResponseEntity<List<SecCatalogEntryDTO>> getCatalog() {
        LOG.debug("REST request to get secured entity catalog");
        List<SecCatalogEntryDTO> result = catalog
            .entries()
            .stream()
            .map(entry -> toDto(entry))
            .toList();
        return ResponseEntity.ok(result);
    }

    private SecCatalogEntryDTO toDto(SecuredEntityEntry entry) {
        List<String> attributes = entityManager
            .getMetamodel()
            .entity(entry.entityClass())
            .getAttributes()
            .stream()
            .map(Attribute::getName)
            .sorted()
            .toList();

        List<String> operations = entry.operations().stream().map(EntityOp::name).sorted().toList();

        SecCatalogEntryDTO dto = new SecCatalogEntryDTO();
        dto.setCode(entry.code());
        dto.setDisplayName(entry.entityClass().getSimpleName());
        dto.setOperations(operations);
        dto.setAttributes(attributes);
        return dto;
    }
}
