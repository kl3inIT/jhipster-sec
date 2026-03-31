package com.vn.core.web.rest;

import com.vn.core.domain.Organization;
import com.vn.core.security.data.UnconstrainedDataManager;
import com.vn.core.security.web.SecuredEntityJsonAdapter;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Benchmark-only baseline endpoint for organization reads.
 */
@RestController
@Profile("api-docs")
@Hidden
@RequestMapping("/api/benchmark/organizations")
@PreAuthorize("isAuthenticated()")
public class BenchmarkOrganizationResource {

    private static final Logger LOG = LoggerFactory.getLogger(BenchmarkOrganizationResource.class);
    private static final String LIST_FETCH_PLAN = "organization-list";
    private static final String DETAIL_FETCH_PLAN = "organization-detail";

    private final UnconstrainedDataManager unconstrainedDataManager;
    private final SecuredEntityJsonAdapter securedEntityJsonAdapter;

    public BenchmarkOrganizationResource(UnconstrainedDataManager unconstrainedDataManager, SecuredEntityJsonAdapter securedEntityJsonAdapter) {
        this.unconstrainedDataManager = unconstrainedDataManager;
        this.securedEntityJsonAdapter = securedEntityJsonAdapter;
    }

    @GetMapping("")
    @Transactional(readOnly = true)
    public ResponseEntity<String> getAllOrganizations(@ParameterObject Pageable pageable) {
        LOG.debug("REST benchmark request to get organizations");
        Page<Organization> page = unconstrainedDataManager.loadPage(Organization.class, null, pageable);
        String json = securedEntityJsonAdapter.toJsonArrayString(page.getContent(), LIST_FETCH_PLAN);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<String> getOrganization(@PathVariable("id") Long id) {
        LOG.debug("REST benchmark request to get organization : {}", id);
        try {
            Organization organization = unconstrainedDataManager.load(Organization.class, id);
            String json = securedEntityJsonAdapter.toJsonString(organization, DETAIL_FETCH_PLAN);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (EntityNotFoundException exception) {
            LOG.debug("Benchmark organization not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }
}
