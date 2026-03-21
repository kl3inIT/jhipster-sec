package com.vn.core.web.rest;

import com.vn.core.service.EmployeeService;
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
 * REST controller exposing secured CRUD endpoints for employees.
 */
@RestController
@RequestMapping("/api/employees")
@PreAuthorize("isAuthenticated()")
public class EmployeeResource {

    private static final Logger LOG = LoggerFactory.getLogger(EmployeeResource.class);

    private final EmployeeService employeeService;

    public EmployeeResource(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("")
    public ResponseEntity<List<Map<String, Object>>> getAllEmployees(@ParameterObject Pageable pageable) {
        LOG.debug("REST request to get employees");
        Page<Map<String, Object>> page = employeeService.list(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getEmployee(@PathVariable("id") Long id) {
        LOG.debug("REST request to get employee : {}", id);
        return ResponseUtil.wrapOrNotFound(employeeService.findOne(id));
    }

    @PostMapping("")
    public ResponseEntity<Map<String, Object>> createEmployee(@RequestBody Map<String, Object> attributes) {
        LOG.debug("REST request to create employee : {}", attributes);
        Map<String, Object> result = employeeService.create(attributes);
        return ResponseEntity.created(URI.create("/api/employees/" + result.get("id"))).body(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateEmployee(@PathVariable("id") Long id, @RequestBody Map<String, Object> attributes) {
        LOG.debug("REST request to update employee : {}", id);
        return ResponseEntity.ok(employeeService.update(id, attributes));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete employee : {}", id);
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
