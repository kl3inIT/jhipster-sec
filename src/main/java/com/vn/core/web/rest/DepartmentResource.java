package com.vn.core.web.rest;

import com.vn.core.domain.Department;
import com.vn.core.security.data.SecureDataManager.EntityMutation;
import com.vn.core.security.web.SecuredEntityJsonAdapter;
import com.vn.core.security.web.SecuredEntityPayloadValidator;
import com.vn.core.service.DepartmentService;
import com.vn.core.web.rest.vm.SecuredEntityQueryVM;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * REST controller exposing secured CRUD endpoints for departments.
 */
@Tag(
    name = "Departments",
    description = "Secured CRUD for Department entities. Responses are attribute-filtered by the caller's VIEW " +
    "permission via SecureEntitySerializerImpl."
)
@RestController
@RequestMapping("/api/departments")
@PreAuthorize("isAuthenticated()")
public class DepartmentResource {

    private static final Logger LOG = LoggerFactory.getLogger(DepartmentResource.class);
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final String LIST_FETCH_PLAN = "department-list";
    private static final String DETAIL_FETCH_PLAN = "department-detail";

    private final DepartmentService departmentService;
    private final SecuredEntityJsonAdapter securedEntityJsonAdapter;
    private final SecuredEntityPayloadValidator securedEntityPayloadValidator;

    public DepartmentResource(
        DepartmentService departmentService,
        SecuredEntityJsonAdapter securedEntityJsonAdapter,
        SecuredEntityPayloadValidator securedEntityPayloadValidator
    ) {
        this.departmentService = departmentService;
        this.securedEntityJsonAdapter = securedEntityJsonAdapter;
        this.securedEntityPayloadValidator = securedEntityPayloadValidator;
    }

    @Operation(
        operationId = "getAllDepartments",
        summary = "List departments (paginated)",
        description = "Returns a paginated list of departments through the @SecuredEntity pipeline. Uses fetch-plan " +
        "'department-list': fields id, code, name, costCenter; nested organization[id, name]. Fields may be " +
        "omitted if the caller lacks VIEW permission."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    type = "array",
                    description = "JSON array of department objects (fetch-plan: department-list). Fields may be " +
                    "omitted based on caller VIEW permissions."
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden — missing entity READ permission", content = @Content),
    })
    @GetMapping("")
    @Transactional(readOnly = true)
    public ResponseEntity<String> getAllDepartments(@ParameterObject Pageable pageable) {
        LOG.debug("REST request to get departments");
        Page<Department> page = departmentService.list(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok()
            .headers(headers)
            .contentType(MediaType.APPLICATION_JSON)
            .body(securedEntityJsonAdapter.toJsonArrayString(page.getContent(), LIST_FETCH_PLAN));
    }

    @Operation(
        operationId = "getDepartment",
        summary = "Get department by ID",
        description = "Returns a single department through the @SecuredEntity pipeline. Uses fetch-plan " +
        "'department-detail': fields id, code, name, costCenter; nested organization[id, code, name, ownerLogin] " +
        "and employees[id, employeeNumber, firstName, lastName, email, salary]. Fields may be omitted if the " +
        "caller lacks VIEW permission."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    type = "object",
                    description = "JSON object with fields from fetch-plan: department-detail. Fields may be omitted " +
                    "based on caller VIEW permissions."
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
        @ApiResponse(responseCode = "404", description = "Department not found", content = @Content),
    })
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<String> getDepartment(@Parameter(description = "Department ID", required = true) @PathVariable("id") Long id) {
        LOG.debug("REST request to get department : {}", id);
        return departmentService
            .findOne(id)
            .map(department ->
                ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(securedEntityJsonAdapter.toJsonString(department, DETAIL_FETCH_PLAN))
            )
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
        operationId = "createDepartment",
        summary = "Create a new department",
        description = "Creates a department through the @SecuredEntity pipeline. Request body is a JSON object with " +
        "writable Department attributes. Attribute-level CREATE permission is enforced on each field. Returns the " +
        "created entity serialized via fetch-plan 'department-detail'."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Created",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(type = "object", description = "Created department (fetch-plan: department-detail).")
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden — missing entity CREATE permission", content = @Content),
    })
    @PostMapping("")
    @Transactional
    public ResponseEntity<String> createDepartment(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Department attributes as JSON object",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(type = "object", example = "{\"code\":\"DEP-001\",\"name\":\"Engineering\",\"costCenter\":\"CC-100\"}")
            )
        ) @RequestBody String attributesJson
    ) {
        LOG.debug("REST request to create department");
        EntityMutation<Department> mutation = securedEntityJsonAdapter.fromJson(attributesJson, Department.class);
        Department result = departmentService.create(mutation);
        return ResponseEntity.created(URI.create("/api/departments/" + result.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .body(securedEntityJsonAdapter.toJsonString(result, DETAIL_FETCH_PLAN));
    }

    @Operation(
        operationId = "updateDepartment",
        summary = "Update an existing department",
        description = "Full update of a department through the @SecuredEntity pipeline. Attribute-level EDIT " +
        "permission is enforced on each provided field. Returns the updated entity serialized via fetch-plan " +
        "'department-detail'."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Updated",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(type = "object", description = "Updated department (fetch-plan: department-detail).")
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden — missing entity UPDATE permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Department not found", content = @Content),
    })
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<String> updateDepartment(
        @Parameter(description = "Department ID", required = true) @PathVariable("id") Long id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Department attributes as JSON object",
            required = true,
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object"))
        ) @RequestBody String attributesJson
    ) {
        LOG.debug("REST request to update department : {}", id);
        EntityMutation<Department> mutation = securedEntityJsonAdapter.fromJson(attributesJson, Department.class);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(securedEntityJsonAdapter.toJsonString(departmentService.update(id, mutation), DETAIL_FETCH_PLAN));
    }

    @Operation(
        operationId = "patchDepartment",
        summary = "Partial update a department",
        description = "PATCH partial update through the @SecuredEntity pipeline. Only provided fields are updated; " +
        "omitted fields are preserved. Attribute-level EDIT permission is enforced on each patched field. Returns " +
        "the updated entity serialized via fetch-plan 'department-detail'."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Updated",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(type = "object", description = "Patched department (fetch-plan: department-detail).")
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
        @ApiResponse(responseCode = "404", description = "Department not found", content = @Content),
    })
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    @Transactional
    public ResponseEntity<String> patchDepartment(
        @Parameter(description = "Department ID", required = true) @PathVariable("id") Long id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Department attributes as JSON object",
            required = true,
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object"))
        ) @RequestBody String attributesJson
    ) {
        LOG.debug("REST request to patch department : {}", id);
        EntityMutation<Department> mutation = securedEntityJsonAdapter.fromJson(attributesJson, Department.class);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(securedEntityJsonAdapter.toJsonString(departmentService.patch(id, mutation), DETAIL_FETCH_PLAN));
    }

    @Operation(
        operationId = "queryDepartments",
        summary = "Query departments with filters",
        description = "Paginated query with optional filters through the @SecuredEntity pipeline. Default fetch-plan: " +
        "'department-list'. Accepts an optional fetchPlanCode in the request body to select a different plan. " +
        "Fields may be omitted if the caller lacks VIEW permission."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(type = "array", description = "JSON array of department objects filtered by query criteria.")
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid query", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
    })
    @PostMapping("/query")
    @Transactional(readOnly = true)
    public ResponseEntity<String> queryDepartments(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Query payload with optional filters, pagination, sort, and fetchPlanCode",
            required = true,
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object"))
        ) @Valid @RequestBody SecuredEntityQueryVM request
    ) {
        LOG.debug("REST request to query departments");
        String fetchPlanCode = resolveFetchPlanCode(request.fetchPlanCode(), LIST_FETCH_PLAN);
        securedEntityPayloadValidator.validateQuery(request, Department.class, fetchPlanCode);
        Page<Department> page = departmentService.query(fetchPlanCode, buildPageable(request), request.filters());
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok()
            .headers(headers)
            .contentType(MediaType.APPLICATION_JSON)
            .body(securedEntityJsonAdapter.toJsonArrayString(page.getContent(), fetchPlanCode));
    }

    @Operation(
        operationId = "deleteDepartment",
        summary = "Delete a department",
        description = "Deletes a department through the @SecuredEntity pipeline. Entity-level DELETE permission is enforced."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Deleted"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden — missing entity DELETE permission", content = @Content),
    })
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteDepartment(@Parameter(description = "Department ID", required = true) @PathVariable("id") Long id) {
        LOG.debug("REST request to delete department : {}", id);
        departmentService.delete(id);
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
