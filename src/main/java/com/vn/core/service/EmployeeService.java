package com.vn.core.service;

import com.vn.core.domain.Department;
import com.vn.core.domain.Employee;
import com.vn.core.repository.DepartmentRepository;
import com.vn.core.security.data.SecureDataManager;
import com.vn.core.security.data.SecureDataManager.EntityMutation;
import com.vn.core.security.data.SecuredLoadQuery;
import jakarta.persistence.EntityNotFoundException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Secured Employee application service backed only by {@link SecureDataManager}.
 */
@Service
@Transactional
public class EmployeeService {

    private static final Logger LOG = LoggerFactory.getLogger(EmployeeService.class);
    private static final Class<Employee> ENTITY_CLASS = Employee.class;
    private static final String ENTITY_CODE = "employee";
    private static final String LIST_FETCH_PLAN = "employee-list";

    private final SecureDataManager secureDataManager;
    private final DepartmentRepository departmentRepository;

    public EmployeeService(SecureDataManager secureDataManager, DepartmentRepository departmentRepository) {
        this.secureDataManager = secureDataManager;
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public Page<Employee> list(Pageable pageable) {
        LOG.debug("Request to list employees");
        return secureDataManager.loadList(ENTITY_CLASS, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Employee> findOne(Long id) {
        LOG.debug("Request to get employee : {}", id);
        return secureDataManager.loadOne(ENTITY_CLASS, id);
    }

    public Employee create(EntityMutation<Employee> mutation) {
        LOG.debug("Request to create employee");
        return secureDataManager.save(ENTITY_CLASS, null, normalizeMutation(mutation));
    }

    public Employee update(Long id, EntityMutation<Employee> mutation) {
        LOG.debug("Request to update employee : {}", id);
        return secureDataManager.save(ENTITY_CLASS, id, normalizeMutation(mutation));
    }

    public Employee patch(Long id, EntityMutation<Employee> mutation) {
        LOG.debug("Request to patch employee : {}", id);
        return secureDataManager.save(ENTITY_CLASS, id, normalizeMutation(mutation));
    }

    @Transactional(readOnly = true)
    public Page<Employee> query(String fetchPlanCode, Pageable pageable, Map<String, Object> filters) {
        LOG.debug("Request to query employees");
        SecuredLoadQuery query = new SecuredLoadQuery(
            ENTITY_CODE,
            null,
            filters,
            pageable,
            pageable.getSort(),
            resolveFetchPlanCode(fetchPlanCode, LIST_FETCH_PLAN)
        );
        return secureDataManager.loadByQuery(ENTITY_CLASS, query);
    }

    public void delete(Long id) {
        LOG.debug("Request to delete employee : {}", id);
        secureDataManager.delete(ENTITY_CLASS, id);
    }

    private String resolveFetchPlanCode(String fetchPlanCode, String defaultFetchPlanCode) {
        return fetchPlanCode == null || fetchPlanCode.isBlank() ? defaultFetchPlanCode : fetchPlanCode;
    }

    private EntityMutation<Employee> normalizeMutation(EntityMutation<Employee> mutation) {
        Employee employee = requireEntity(mutation);
        adaptDepartmentReference(employee, mutation.changedAttributes());
        return mutation;
    }

    private Employee requireEntity(EntityMutation<Employee> mutation) {
        if (mutation == null || mutation.entity() == null) {
            throw new IllegalArgumentException("Typed employee mutation is required");
        }
        return mutation.entity();
    }

    private void adaptDepartmentReference(Employee employee, Collection<String> changedAttributes) {
        if (changedAttributes == null || !changedAttributes.contains("department")) {
            return;
        }

        Department requestedDepartment = employee.getDepartment();
        Long departmentId = requestedDepartment != null ? requestedDepartment.getId() : null;
        if (departmentId == null) {
            throw new IllegalArgumentException("employee.department reference requires an id");
        }

        secureDataManager
            .loadOne(Department.class, departmentId)
            .orElseThrow(() -> new AccessDeniedException("Department reference not found or not accessible: " + departmentId));

        Department department = departmentRepository
            .findById(departmentId)
            .orElseThrow(() -> new EntityNotFoundException("Department not found: " + departmentId));
        employee.setDepartment(department);
    }
}
