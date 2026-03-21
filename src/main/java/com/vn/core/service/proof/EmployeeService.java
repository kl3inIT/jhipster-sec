package com.vn.core.service.proof;

import com.vn.core.security.data.SecuredLoadQuery;
import com.vn.core.security.data.SecureDataManager;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Secured Employee application service backed only by {@link SecureDataManager}.
 */
@Service
@Transactional
public class EmployeeService {

    private static final Logger LOG = LoggerFactory.getLogger(EmployeeService.class);

    private static final String ENTITY_CODE = "employee";
    private static final String LIST_FETCH_PLAN = "employee-list";
    private static final String DETAIL_FETCH_PLAN = "employee-detail";

    private final SecureDataManager secureDataManager;

    public EmployeeService(SecureDataManager secureDataManager) {
        this.secureDataManager = secureDataManager;
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> list(Pageable pageable) {
        LOG.debug("Request to list proof employees");
        return secureDataManager.loadByQuery(SecuredLoadQuery.of(ENTITY_CODE, LIST_FETCH_PLAN, pageable));
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> findOne(Long id) {
        LOG.debug("Request to get proof employee : {}", id);
        return secureDataManager.loadOne(ENTITY_CODE, id, DETAIL_FETCH_PLAN);
    }

    public Map<String, Object> create(Map<String, Object> attributes) {
        LOG.debug("Request to create proof employee : {}", attributes);
        return secureDataManager.save(ENTITY_CODE, null, attributes, DETAIL_FETCH_PLAN);
    }

    public Map<String, Object> update(Long id, Map<String, Object> attributes) {
        LOG.debug("Request to update proof employee : {}", id);
        return secureDataManager.save(ENTITY_CODE, id, attributes, DETAIL_FETCH_PLAN);
    }

    public void delete(Long id) {
        LOG.debug("Request to delete proof employee : {}", id);
        secureDataManager.delete(ENTITY_CODE, id);
    }
}
