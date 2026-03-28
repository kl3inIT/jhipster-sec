package com.vn.core.service;

import com.vn.core.domain.Department;
import com.vn.core.domain.Organization;
import com.vn.core.repository.OrganizationRepository;
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
 * Secured Department application service backed only by {@link SecureDataManager}.
 */
@Service
@Transactional
public class DepartmentService {

    private static final Logger LOG = LoggerFactory.getLogger(DepartmentService.class);
    private static final Class<Department> ENTITY_CLASS = Department.class;
    private static final String ENTITY_CODE = "department";
    private static final String LIST_FETCH_PLAN = "department-list";

    private final SecureDataManager secureDataManager;
    private final OrganizationRepository organizationRepository;

    public DepartmentService(SecureDataManager secureDataManager, OrganizationRepository organizationRepository) {
        this.secureDataManager = secureDataManager;
        this.organizationRepository = organizationRepository;
    }

    @Transactional(readOnly = true)
    public Page<Department> list(Pageable pageable) {
        LOG.debug("Request to list departments");
        return secureDataManager.loadList(ENTITY_CLASS, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Department> findOne(Long id) {
        LOG.debug("Request to get department : {}", id);
        return secureDataManager.loadOne(ENTITY_CLASS, id);
    }

    public Department create(EntityMutation<Department> mutation) {
        LOG.debug("Request to create department");
        return secureDataManager.save(ENTITY_CLASS, null, normalizeMutation(mutation));
    }

    public Department update(Long id, EntityMutation<Department> mutation) {
        LOG.debug("Request to update department : {}", id);
        return secureDataManager.save(ENTITY_CLASS, id, normalizeMutation(mutation));
    }

    public Department patch(Long id, EntityMutation<Department> mutation) {
        LOG.debug("Request to patch department : {}", id);
        return secureDataManager.save(ENTITY_CLASS, id, normalizeMutation(mutation));
    }

    @Transactional(readOnly = true)
    public Page<Department> query(String fetchPlanCode, Pageable pageable, Map<String, Object> filters) {
        LOG.debug("Request to query departments");
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
        LOG.debug("Request to delete department : {}", id);
        secureDataManager.delete(ENTITY_CLASS, id);
    }

    private String resolveFetchPlanCode(String fetchPlanCode, String defaultFetchPlanCode) {
        return fetchPlanCode == null || fetchPlanCode.isBlank() ? defaultFetchPlanCode : fetchPlanCode;
    }

    private EntityMutation<Department> normalizeMutation(EntityMutation<Department> mutation) {
        Department department = requireEntity(mutation);
        adaptOrganizationReference(department, mutation.changedAttributes());
        return mutation;
    }

    private Department requireEntity(EntityMutation<Department> mutation) {
        if (mutation == null || mutation.entity() == null) {
            throw new IllegalArgumentException("Typed department mutation is required");
        }
        return mutation.entity();
    }

    private void adaptOrganizationReference(Department department, Collection<String> changedAttributes) {
        if (changedAttributes == null || !changedAttributes.contains("organization")) {
            return;
        }

        Organization requestedOrganization = department.getOrganization();
        Long organizationId = requestedOrganization != null ? requestedOrganization.getId() : null;
        if (organizationId == null) {
            throw new IllegalArgumentException("department.organization reference requires an id");
        }

        secureDataManager
            .loadOne(Organization.class, organizationId)
            .orElseThrow(() -> new AccessDeniedException("Organization reference not found or not accessible: " + organizationId));

        Organization organization = organizationRepository
            .findById(organizationId)
            .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + organizationId));
        department.setOrganization(organization);
    }
}
