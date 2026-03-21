package com.vn.core.web.rest.proof;

import com.vn.core.service.proof.DepartmentService;
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
 * REST controller exposing secured CRUD endpoints for proof departments.
 */
@RestController
@RequestMapping("/api/proof/departments")
@PreAuthorize("isAuthenticated()")
public class DepartmentResource {

    private static final Logger LOG = LoggerFactory.getLogger(DepartmentResource.class);

    private final DepartmentService departmentService;

    public DepartmentResource(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping("")
    public ResponseEntity<List<Map<String, Object>>> getAllDepartments(@ParameterObject Pageable pageable) {
        LOG.debug("REST request to get proof departments");
        Page<Map<String, Object>> page = departmentService.list(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDepartment(@PathVariable("id") Long id) {
        LOG.debug("REST request to get proof department : {}", id);
        return ResponseUtil.wrapOrNotFound(departmentService.findOne(id));
    }

    @PostMapping("")
    public ResponseEntity<Map<String, Object>> createDepartment(@RequestBody Map<String, Object> attributes) {
        LOG.debug("REST request to create proof department : {}", attributes);
        Map<String, Object> result = departmentService.create(attributes);
        return ResponseEntity.created(URI.create("/api/proof/departments/" + result.get("id"))).body(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateDepartment(@PathVariable("id") Long id, @RequestBody Map<String, Object> attributes) {
        LOG.debug("REST request to update proof department : {}", id);
        return ResponseEntity.ok(departmentService.update(id, attributes));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete proof department : {}", id);
        departmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
