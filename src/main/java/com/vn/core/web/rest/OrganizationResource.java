package com.vn.core.web.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.core.service.OrganizationService;
import com.vn.core.web.rest.vm.SecuredEntityQueryVM;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

/**
 * REST controller exposing secured CRUD endpoints for organizations.
 */
@RestController
@RequestMapping("/api/organizations")
@PreAuthorize("isAuthenticated()")
public class OrganizationResource {

    private static final Logger LOG = LoggerFactory.getLogger(OrganizationResource.class);
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private final OrganizationService organizationService;
    private final ObjectMapper objectMapper;

    public OrganizationResource(OrganizationService organizationService, ObjectMapper objectMapper) {
        this.organizationService = organizationService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("")
    public ResponseEntity<String> getAllOrganizations(@ParameterObject Pageable pageable) {
        LOG.debug("REST request to get organizations");
        Page<JsonNode> page = organizationService.list(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_JSON).body(writeJson(page.getContent()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> getOrganization(@PathVariable("id") Long id) {
        LOG.debug("REST request to get organization : {}", id);
        return organizationService
            .findOne(id)
            .map(body -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(writeJson(body)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("")
    public ResponseEntity<String> createOrganization(@RequestBody String attributesJson) {
        JsonNode attributes = parseObject(attributesJson);
        LOG.debug("REST request to create organization : {}", attributes);
        JsonNode result = organizationService.create(attributes);
        return ResponseEntity
            .created(URI.create("/api/organizations/" + result.path("id").asText()))
            .contentType(MediaType.APPLICATION_JSON)
            .body(writeJson(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateOrganization(@PathVariable("id") Long id, @RequestBody String attributesJson) {
        JsonNode attributes = parseObject(attributesJson);
        LOG.debug("REST request to update organization : {}", id);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(writeJson(organizationService.update(id, attributes)));
    }

    @PostMapping("/query")
    public ResponseEntity<String> queryOrganizations(@RequestBody SecuredEntityQueryVM request) {
        LOG.debug("REST request to query organizations");
        Page<JsonNode> page = organizationService.query(request.fetchPlanCode(), buildPageable(request), request.filters());
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_JSON).body(writeJson(page.getContent()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrganization(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete organization : {}", id);
        organizationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Pageable buildPageable(SecuredEntityQueryVM request) {
        int page = request.page() != null && request.page() >= 0 ? request.page() : DEFAULT_PAGE;
        int size = request.size() != null && request.size() > 0 ? request.size() : DEFAULT_SIZE;
        return PageRequest.of(page, size, buildSort(request.sort()));
    }

    private Sort buildSort(List<String> sortValues) {
        if (sortValues == null || sortValues.isEmpty()) {
            return Sort.unsorted();
        }

        List<Sort.Order> orders = new ArrayList<>();
        for (String sortValue : sortValues) {
            if (sortValue == null || sortValue.isBlank()) {
                continue;
            }

            String[] parts = sortValue.split(",", 2);
            String property = parts[0].trim();
            if (property.isEmpty()) {
                continue;
            }

            Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
            orders.add(new Sort.Order(direction, property));
        }

        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }

    private JsonNode parseObject(String body) {
        try {
            JsonNode parsed = objectMapper.readTree(body);
            if (parsed == null || !parsed.isObject()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body must be a JSON object");
            }
            return parsed;
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body must be valid JSON", ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize organization response", ex);
        }
    }
}
