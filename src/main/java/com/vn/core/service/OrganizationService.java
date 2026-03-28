package com.vn.core.service;

import com.vn.core.domain.Organization;
import com.vn.core.security.data.SecureDataManager;
import com.vn.core.security.data.SecureDataManager.EntityMutation;
import com.vn.core.security.data.SecuredLoadQuery;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Secured Organization application service backed only by {@link SecureDataManager}.
 */
@Service
@Transactional
public class OrganizationService {

    private static final Logger LOG = LoggerFactory.getLogger(OrganizationService.class);
    private static final Class<Organization> ENTITY_CLASS = Organization.class;
    private static final String ENTITY_CODE = "organization";
    private static final String LIST_FETCH_PLAN = "organization-list";

    private final SecureDataManager secureDataManager;

    public OrganizationService(SecureDataManager secureDataManager) {
        this.secureDataManager = secureDataManager;
    }

    @Transactional(readOnly = true)
    public Page<Organization> list(Pageable pageable) {
        LOG.debug("Request to list organizations");
        return secureDataManager.loadList(ENTITY_CLASS, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Organization> findOne(Long id) {
        LOG.debug("Request to get organization : {}", id);
        return secureDataManager.loadOne(ENTITY_CLASS, id);
    }

    public Organization create(EntityMutation<Organization> mutation) {
        LOG.debug("Request to create organization");
        return secureDataManager.save(ENTITY_CLASS, null, mutation);
    }

    public Organization update(Long id, EntityMutation<Organization> mutation) {
        LOG.debug("Request to update organization : {}", id);
        return secureDataManager.save(ENTITY_CLASS, id, mutation);
    }

    public Organization patch(Long id, EntityMutation<Organization> mutation) {
        LOG.debug("Request to patch organization : {}", id);
        return secureDataManager.save(ENTITY_CLASS, id, mutation);
    }

    @Transactional(readOnly = true)
    public Page<Organization> query(String fetchPlanCode, Pageable pageable, Map<String, Object> filters) {
        LOG.debug("Request to query organizations");
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
        LOG.debug("Request to delete organization : {}", id);
        secureDataManager.delete(ENTITY_CLASS, id);
    }

    private String resolveFetchPlanCode(String fetchPlanCode, String defaultFetchPlanCode) {
        return fetchPlanCode == null || fetchPlanCode.isBlank() ? defaultFetchPlanCode : fetchPlanCode;
    }
}
