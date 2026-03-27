package com.vn.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.core.security.data.SecureDataManager;
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
    private static final TypeReference<Map<String, Object>> ATTRIBUTE_MAP_TYPE = new TypeReference<>() {};

    private static final String ENTITY_CODE = "organization";
    private static final String LIST_FETCH_PLAN = "organization-list";
    private static final String DETAIL_FETCH_PLAN = "organization-detail";

    private final SecureDataManager secureDataManager;
    private final ObjectMapper objectMapper;

    public OrganizationService(SecureDataManager secureDataManager, ObjectMapper objectMapper) {
        this.secureDataManager = secureDataManager;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Page<JsonNode> list(Pageable pageable) {
        LOG.debug("Request to list organizations");
        return secureDataManager.loadList(ENTITY_CODE, LIST_FETCH_PLAN, pageable).map(objectMapper::valueToTree);
    }

    @Transactional(readOnly = true)
    public Optional<JsonNode> findOne(Long id) {
        LOG.debug("Request to get organization : {}", id);
        return secureDataManager.loadOne(ENTITY_CODE, id, DETAIL_FETCH_PLAN).map(objectMapper::valueToTree);
    }

    public JsonNode create(JsonNode attributes) {
        LOG.debug("Request to create organization : {}", attributes);
        return objectMapper.valueToTree(secureDataManager.save(ENTITY_CODE, null, toAttributeMap(attributes), DETAIL_FETCH_PLAN));
    }

    public JsonNode update(Long id, JsonNode attributes) {
        LOG.debug("Request to update organization : {}", id);
        return objectMapper.valueToTree(secureDataManager.save(ENTITY_CODE, id, toAttributeMap(attributes), DETAIL_FETCH_PLAN));
    }

    public JsonNode patch(Long id, JsonNode attributes) {
        LOG.debug("Request to patch organization : {}", id);
        return objectMapper.valueToTree(secureDataManager.save(ENTITY_CODE, id, toAttributeMap(attributes), DETAIL_FETCH_PLAN));
    }

    @Transactional(readOnly = true)
    public Page<JsonNode> query(String fetchPlanCode, Pageable pageable, Map<String, Object> filters) {
        LOG.debug("Request to query organizations");
        SecuredLoadQuery query = new SecuredLoadQuery(
            ENTITY_CODE,
            null,
            filters,
            pageable,
            pageable.getSort(),
            resolveFetchPlanCode(fetchPlanCode, LIST_FETCH_PLAN)
        );
        return secureDataManager.loadByQuery(query).map(objectMapper::valueToTree);
    }

    public void delete(Long id) {
        LOG.debug("Request to delete organization : {}", id);
        secureDataManager.delete(ENTITY_CODE, id);
    }

    private String resolveFetchPlanCode(String fetchPlanCode, String defaultFetchPlanCode) {
        return fetchPlanCode == null || fetchPlanCode.isBlank() ? defaultFetchPlanCode : fetchPlanCode;
    }

    private Map<String, Object> toAttributeMap(JsonNode attributes) {
        return objectMapper.convertValue(attributes, ATTRIBUTE_MAP_TYPE);
    }
}
