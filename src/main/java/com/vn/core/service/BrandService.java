package com.vn.core.service;

import com.vn.core.domain.Brand;
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
 * Secured Brand application service backed only by {@link SecureDataManager}.
 */
@Service
@Transactional
public class BrandService {

    private static final Logger LOG = LoggerFactory.getLogger(BrandService.class);
    private static final Class<Brand> ENTITY_CLASS = Brand.class;
    private static final String ENTITY_CODE = "brand";
    private static final String LIST_FETCH_PLAN = "brand-list";

    private final SecureDataManager secureDataManager;

    public BrandService(SecureDataManager secureDataManager) {
        this.secureDataManager = secureDataManager;
    }

    @Transactional(readOnly = true)
    public Page<Brand> list(Pageable pageable) {
        LOG.debug("Request to list brands");
        return secureDataManager.loadList(ENTITY_CLASS, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Brand> findOne(Long id) {
        LOG.debug("Request to get brand : {}", id);
        return secureDataManager.loadOne(ENTITY_CLASS, id);
    }

    public Brand create(EntityMutation<Brand> mutation) {
        LOG.debug("Request to create brand");
        return secureDataManager.save(ENTITY_CLASS, null, mutation);
    }

    public Brand update(Long id, EntityMutation<Brand> mutation) {
        LOG.debug("Request to update brand : {}", id);
        return secureDataManager.save(ENTITY_CLASS, id, mutation);
    }

    public Brand patch(Long id, EntityMutation<Brand> mutation) {
        LOG.debug("Request to patch brand : {}", id);
        return secureDataManager.save(ENTITY_CLASS, id, mutation);
    }

    @Transactional(readOnly = true)
    public Page<Brand> query(String fetchPlanCode, Pageable pageable, Map<String, Object> filters) {
        LOG.debug("Request to query brands");
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
        LOG.debug("Request to delete brand : {}", id);
        secureDataManager.delete(ENTITY_CLASS, id);
    }

    private String resolveFetchPlanCode(String fetchPlanCode, String defaultFetchPlanCode) {
        return fetchPlanCode == null || fetchPlanCode.isBlank() ? defaultFetchPlanCode : fetchPlanCode;
    }
}
