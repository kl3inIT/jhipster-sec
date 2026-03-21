package com.vn.core.web.rest.proof;

import com.vn.core.service.proof.OrganizationService;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller exposing secured CRUD endpoints for proof organizations.
 */
@RestController
@RequestMapping("/api/proof/organizations")
@PreAuthorize("isAuthenticated()")
public class OrganizationResource {

    private static final Logger LOG = LoggerFactory.getLogger(OrganizationResource.class);

    private final OrganizationService organizationService;

    public OrganizationResource(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("")
    public ResponseEntity<List<Map<String, Object>>> getAllOrganizations(@ParameterObject Pageable pageable) {
        LOG.debug("REST request to get proof organizations");
        Page<Map<String, Object>> page = organizationService.list(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOrganization(@PathVariable("id") Long id) {
        LOG.debug("REST request to get proof organization : {}", id);
        return ResponseUtil.wrapOrNotFound(organizationService.findOne(id));
    }

    @PostMapping("")
    public ResponseEntity<Map<String, Object>> createOrganization(@RequestBody Map<String, Object> attributes) {
        LOG.debug("REST request to create proof organization : {}", attributes);
        Map<String, Object> result = organizationService.create(attributes);
        return ResponseEntity.created(URI.create("/api/proof/organizations/" + result.get("id"))).body(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateOrganization(@PathVariable("id") Long id, @RequestBody Map<String, Object> attributes) {
        LOG.debug("REST request to update proof organization : {}", id);
        return ResponseEntity.ok(organizationService.update(id, attributes));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrganization(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete proof organization : {}", id);
        organizationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
