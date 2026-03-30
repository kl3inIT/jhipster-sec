package com.vn.core.web.rest;

import com.vn.core.domain.ShoeVariant;
import com.vn.core.security.data.SecureDataManager.EntityMutation;
import com.vn.core.security.web.SecuredEntityJsonAdapter;
import com.vn.core.security.web.SecuredEntityPayloadValidator;
import com.vn.core.service.ShoeVariantService;
import com.vn.core.web.rest.vm.SecuredEntityQueryVM;
import jakarta.validation.Valid;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

/**
 * REST controller exposing secured CRUD endpoints for shoeVariants.
 */
@RestController
@RequestMapping("/api/shoe-variants")
@PreAuthorize("isAuthenticated()")
public class ShoeVariantResource {

    private static final Logger LOG = LoggerFactory.getLogger(ShoeVariantResource.class);
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final String LIST_FETCH_PLAN = "shoevariant-list";
    private static final String DETAIL_FETCH_PLAN = "shoevariant-detail";

    private final ShoeVariantService shoeVariantService;
    private final SecuredEntityJsonAdapter securedEntityJsonAdapter;
    private final SecuredEntityPayloadValidator securedEntityPayloadValidator;

    public ShoeVariantResource(
        ShoeVariantService shoeVariantService,
        SecuredEntityJsonAdapter securedEntityJsonAdapter,
        SecuredEntityPayloadValidator securedEntityPayloadValidator
    ) {
        this.shoeVariantService = shoeVariantService;
        this.securedEntityJsonAdapter = securedEntityJsonAdapter;
        this.securedEntityPayloadValidator = securedEntityPayloadValidator;
    }

    @GetMapping("")
    @Transactional(readOnly = true)
    public ResponseEntity<String> getAllShoeVariants(@ParameterObject Pageable pageable) {
        LOG.debug("REST request to get shoeVariants");
        Page<ShoeVariant> page = shoeVariantService.list(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok()
            .headers(headers)
            .contentType(MediaType.APPLICATION_JSON)
            .body(securedEntityJsonAdapter.toJsonArrayString(page.getContent(), LIST_FETCH_PLAN));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<String> getShoeVariant(@PathVariable("id") Long id) {
        LOG.debug("REST request to get shoeVariant : {}", id);
        return shoeVariantService
            .findOne(id)
            .map(shoeVariant ->
                ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(securedEntityJsonAdapter.toJsonString(shoeVariant, DETAIL_FETCH_PLAN))
            )
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("")
    @Transactional
    public ResponseEntity<String> createShoeVariant(@RequestBody String attributesJson) {
        LOG.debug("REST request to create shoeVariant");
        EntityMutation<ShoeVariant> mutation = securedEntityJsonAdapter.fromJson(attributesJson, ShoeVariant.class);
        ShoeVariant result = shoeVariantService.create(mutation);
        return ResponseEntity.created(URI.create("/api/shoe-variants/" + result.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .body(securedEntityJsonAdapter.toJsonString(result, DETAIL_FETCH_PLAN));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<String> updateShoeVariant(@PathVariable("id") Long id, @RequestBody String attributesJson) {
        LOG.debug("REST request to update shoeVariant : {}", id);
        EntityMutation<ShoeVariant> mutation = securedEntityJsonAdapter.fromJson(attributesJson, ShoeVariant.class);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(securedEntityJsonAdapter.toJsonString(shoeVariantService.update(id, mutation), DETAIL_FETCH_PLAN));
    }

    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    @Transactional
    public ResponseEntity<String> patchShoeVariant(@PathVariable("id") Long id, @RequestBody String attributesJson) {
        LOG.debug("REST request to patch shoeVariant : {}", id);
        EntityMutation<ShoeVariant> mutation = securedEntityJsonAdapter.fromJson(attributesJson, ShoeVariant.class);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(securedEntityJsonAdapter.toJsonString(shoeVariantService.patch(id, mutation), DETAIL_FETCH_PLAN));
    }

    @PostMapping("/query")
    @Transactional(readOnly = true)
    public ResponseEntity<String> queryShoeVariants(@Valid @RequestBody SecuredEntityQueryVM request) {
        LOG.debug("REST request to query shoeVariants");
        String fetchPlanCode = resolveFetchPlanCode(request.fetchPlanCode(), LIST_FETCH_PLAN);
        securedEntityPayloadValidator.validateQuery(request, ShoeVariant.class, fetchPlanCode);
        Page<ShoeVariant> page = shoeVariantService.query(fetchPlanCode, buildPageable(request), request.filters());
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok()
            .headers(headers)
            .contentType(MediaType.APPLICATION_JSON)
            .body(securedEntityJsonAdapter.toJsonArrayString(page.getContent(), fetchPlanCode));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteShoeVariant(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete shoeVariant : {}", id);
        shoeVariantService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private String resolveFetchPlanCode(String fetchPlanCode, String defaultFetchPlanCode) {
        return fetchPlanCode == null || fetchPlanCode.isBlank() ? defaultFetchPlanCode : fetchPlanCode;
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

            Sort.Direction direction =
                parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()) ? Sort.Direction.DESC : Sort.Direction.ASC;
            orders.add(new Sort.Order(direction, property));
        }

        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }
}
