package com.vn.core.web.rest;

import com.vn.core.service.BenchmarkOrganizationStandardService;
import com.vn.core.service.dto.OrganizationDTO;
import com.vn.core.service.dto.OrganizationDetailDTO;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * Benchmark-only baseline endpoint for organization reads.
 */
@RestController
@Profile("api-docs")
@Hidden
@RequestMapping("/api/benchmark/organizations-standard")
@PreAuthorize("isAuthenticated()")
public class BenchmarkOrganizationResource {

    private static final Logger LOG = LoggerFactory.getLogger(BenchmarkOrganizationResource.class);

    private final BenchmarkOrganizationStandardService benchmarkOrganizationStandardService;

    public BenchmarkOrganizationResource(BenchmarkOrganizationStandardService benchmarkOrganizationStandardService) {
        this.benchmarkOrganizationStandardService = benchmarkOrganizationStandardService;
    }

    @GetMapping("")
    @Transactional(readOnly = true)
    public ResponseEntity<List<OrganizationDTO>> getAllOrganizations(@ParameterObject Pageable pageable) {
        LOG.debug("REST benchmark request to get organizations");
        Page<OrganizationDTO> page = benchmarkOrganizationStandardService.list(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<OrganizationDetailDTO> getOrganization(@PathVariable("id") Long id) {
        LOG.debug("REST benchmark request to get organization : {}", id);
        Optional<OrganizationDetailDTO> organizationDetailDTO = benchmarkOrganizationStandardService.findOne(id);
        return ResponseUtil.wrapOrNotFound(organizationDetailDTO);
    }
}
