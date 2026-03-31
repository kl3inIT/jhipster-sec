package com.vn.core.web.rest;

import com.vn.core.domain.Employee;
import com.vn.core.security.data.SecureDataManager.EntityMutation;
import com.vn.core.security.web.SecuredEntityJsonAdapter;
import com.vn.core.security.web.SecuredEntityPayloadValidator;
import com.vn.core.service.EmployeeService;
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
 * REST controller exposing secured CRUD endpoints for employees.
 */
@Tag(
    name = "Employees",
    description = "Secured CRUD for Employee entities. Responses are attribute-filtered by the caller's VIEW " +
    "permission via SecureEntitySerializerImpl."
)
@RestController
@RequestMapping("/api/employees")
@PreAuthorize("isAuthenticated()")
public class EmployeeResource {

    private static final Logger LOG = LoggerFactory.getLogger(EmployeeResource.class);
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final String LIST_FETCH_PLAN = "employee-list";
    private static final String DETAIL_FETCH_PLAN = "employee-detail";

    private final EmployeeService employeeService;
    private final SecuredEntityJsonAdapter securedEntityJsonAdapter;
    private final SecuredEntityPayloadValidator securedEntityPayloadValidator;

    public EmployeeResource(
        EmployeeService employeeService,
        SecuredEntityJsonAdapter securedEntityJsonAdapter,
        SecuredEntityPayloadValidator securedEntityPayloadValidator
    ) {
        this.employeeService = employeeService;
        this.securedEntityJsonAdapter = securedEntityJsonAdapter;
        this.securedEntityPayloadValidator = securedEntityPayloadValidator;
    }

    @Operation(
        operationId = "getAllEmployees",
        summary = "List employees (paginated)",
        description = "Returns a paginated list of employees through the @SecuredEntity pipeline. Uses fetch-plan " +
        "'employee-list': fields id, employeeNumber, firstName, lastName, email; nested department[id, name]. " +
        "Fields may be omitted if the caller lacks VIEW permission."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    type = "array",
                    description = "JSON array of employee objects (fetch-plan: employee-list). Fields may be omitted " +
                    "based on caller VIEW permissions."
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden — missing entity READ permission", content = @Content),
    })
    @GetMapping("")
    @Transactional(readOnly = true)
    public ResponseEntity<String> getAllEmployees(@ParameterObject Pageable pageable) {
        LOG.debug("REST request to get employees");
        Page<Employee> page = employeeService.list(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok()
            .headers(headers)
            .contentType(MediaType.APPLICATION_JSON)
            .body(securedEntityJsonAdapter.toJsonArrayString(page.getContent(), LIST_FETCH_PLAN));
    }

    @Operation(
        operationId = "getEmployee",
        summary = "Get employee by ID",
        description = "Returns a single employee through the @SecuredEntity pipeline. Uses fetch-plan " +
        "'employee-detail': fields id, employeeNumber, firstName, lastName, email, salary; nested department[id, " +
        "code, name]. Fields may be omitted if the caller lacks VIEW permission."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    type = "object",
                    description = "JSON object with fields from fetch-plan: employee-detail. Fields may be omitted " +
                    "based on caller VIEW permissions."
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
        @ApiResponse(responseCode = "404", description = "Employee not found", content = @Content),
    })
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<String> getEmployee(@Parameter(description = "Employee ID", required = true) @PathVariable("id") Long id) {
        LOG.debug("REST request to get employee : {}", id);
        return employeeService
            .findOne(id)
            .map(employee ->
                ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(securedEntityJsonAdapter.toJsonString(employee, DETAIL_FETCH_PLAN))
            )
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
        operationId = "createEmployee",
        summary = "Create a new employee",
        description = "Creates an employee through the @SecuredEntity pipeline. Request body is a JSON object with " +
        "writable Employee attributes. Attribute-level CREATE permission is enforced on each field. Returns the " +
        "created entity serialized via fetch-plan 'employee-detail'."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Created",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(type = "object", description = "Created employee (fetch-plan: employee-detail).")
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden — missing entity CREATE permission", content = @Content),
    })
    @PostMapping("")
    @Transactional
    public ResponseEntity<String> createEmployee(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Employee attributes as JSON object",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    type = "object",
                    example = "{\"employeeNumber\":\"EMP-001\",\"firstName\":\"Ada\",\"lastName\":\"Lovelace\",\"email\":\"ada@example.com\"}"
                )
            )
        ) @RequestBody String attributesJson
    ) {
        LOG.debug("REST request to create employee");
        EntityMutation<Employee> mutation = securedEntityJsonAdapter.fromJson(attributesJson, Employee.class);
        Employee result = employeeService.create(mutation);
        return ResponseEntity.created(URI.create("/api/employees/" + result.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .body(securedEntityJsonAdapter.toJsonString(result, DETAIL_FETCH_PLAN));
    }

    @Operation(
        operationId = "updateEmployee",
        summary = "Update an existing employee",
        description = "Full update of an employee through the @SecuredEntity pipeline. Attribute-level EDIT " +
        "permission is enforced on each provided field. Returns the updated entity serialized via fetch-plan " +
        "'employee-detail'."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Updated",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(type = "object", description = "Updated employee (fetch-plan: employee-detail).")
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden — missing entity UPDATE permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Employee not found", content = @Content),
    })
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<String> updateEmployee(
        @Parameter(description = "Employee ID", required = true) @PathVariable("id") Long id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Employee attributes as JSON object",
            required = true,
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object"))
        ) @RequestBody String attributesJson
    ) {
        LOG.debug("REST request to update employee : {}", id);
        EntityMutation<Employee> mutation = securedEntityJsonAdapter.fromJson(attributesJson, Employee.class);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(securedEntityJsonAdapter.toJsonString(employeeService.update(id, mutation), DETAIL_FETCH_PLAN));
    }

    @Operation(
        operationId = "patchEmployee",
        summary = "Partial update an employee",
        description = "PATCH partial update through the @SecuredEntity pipeline. Only provided fields are updated; " +
        "omitted fields are preserved. Attribute-level EDIT permission is enforced on each patched field. Returns " +
        "the updated entity serialized via fetch-plan 'employee-detail'."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Updated",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(type = "object", description = "Patched employee (fetch-plan: employee-detail).")
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
        @ApiResponse(responseCode = "404", description = "Employee not found", content = @Content),
    })
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    @Transactional
    public ResponseEntity<String> patchEmployee(
        @Parameter(description = "Employee ID", required = true) @PathVariable("id") Long id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Employee attributes as JSON object",
            required = true,
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object"))
        ) @RequestBody String attributesJson
    ) {
        LOG.debug("REST request to patch employee : {}", id);
        EntityMutation<Employee> mutation = securedEntityJsonAdapter.fromJson(attributesJson, Employee.class);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(securedEntityJsonAdapter.toJsonString(employeeService.patch(id, mutation), DETAIL_FETCH_PLAN));
    }

    @Operation(
        operationId = "queryEmployees",
        summary = "Query employees with filters",
        description = "Paginated query with optional filters through the @SecuredEntity pipeline. Default fetch-plan: " +
        "'employee-list'. Accepts an optional fetchPlanCode in the request body to select a different plan. Fields " +
        "may be omitted if the caller lacks VIEW permission."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(type = "array", description = "JSON array of employee objects filtered by query criteria.")
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid query", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
    })
    @PostMapping("/query")
    @Transactional(readOnly = true)
    public ResponseEntity<String> queryEmployees(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Query payload with optional filters, pagination, sort, and fetchPlanCode",
            required = true,
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object"))
        ) @Valid @RequestBody SecuredEntityQueryVM request
    ) {
        LOG.debug("REST request to query employees");
        String fetchPlanCode = resolveFetchPlanCode(request.fetchPlanCode(), LIST_FETCH_PLAN);
        securedEntityPayloadValidator.validateQuery(request, Employee.class, fetchPlanCode);
        Page<Employee> page = employeeService.query(fetchPlanCode, buildPageable(request), request.filters());
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok()
            .headers(headers)
            .contentType(MediaType.APPLICATION_JSON)
            .body(securedEntityJsonAdapter.toJsonArrayString(page.getContent(), fetchPlanCode));
    }

    @Operation(
        operationId = "deleteEmployee",
        summary = "Delete an employee",
        description = "Deletes an employee through the @SecuredEntity pipeline. Entity-level DELETE permission is enforced."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Deleted"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden — missing entity DELETE permission", content = @Content),
    })
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteEmployee(@Parameter(description = "Employee ID", required = true) @PathVariable("id") Long id) {
        LOG.debug("REST request to delete employee : {}", id);
        employeeService.delete(id);
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
