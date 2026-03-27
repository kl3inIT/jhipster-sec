package com.vn.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.core.domain.Organization;
import com.vn.core.repository.OrganizationRepository;
import com.vn.core.security.data.SecureDataManager;
import com.vn.core.security.data.SecuredLoadQuery;
import jakarta.persistence.EntityNotFoundException;
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
    private static final TypeReference<Map<String, Object>> ATTRIBUTE_MAP_TYPE = new TypeReference<>() {};

    private static final String ENTITY_CODE = "department";
    private static final String LIST_FETCH_PLAN = "department-list";
    private static final String DETAIL_FETCH_PLAN = "department-detail";
    private static final String ORGANIZATION_ENTITY_CODE = "organization";
    private static final String ORGANIZATION_REFERENCE_FIELD = "organization";
    private static final String ORGANIZATION_REFERENCE_FETCH_PLAN = "organization-list";

    private final SecureDataManager secureDataManager;
    private final ObjectMapper objectMapper;
    private final OrganizationRepository organizationRepository;

    public DepartmentService(
        SecureDataManager secureDataManager,
        ObjectMapper objectMapper,
        OrganizationRepository organizationRepository
    ) {
        this.secureDataManager = secureDataManager;
        this.objectMapper = objectMapper;
        this.organizationRepository = organizationRepository;
    }

    @Transactional(readOnly = true)
    public Page<JsonNode> list(Pageable pageable) {
        LOG.debug("Request to list departments");
        return secureDataManager.loadList(ENTITY_CODE, LIST_FETCH_PLAN, pageable).map(objectMapper::valueToTree);
    }

    @Transactional(readOnly = true)
    public Optional<JsonNode> findOne(Long id) {
        LOG.debug("Request to get department : {}", id);
        return secureDataManager.loadOne(ENTITY_CODE, id, DETAIL_FETCH_PLAN).map(objectMapper::valueToTree);
    }

    public JsonNode create(JsonNode attributes) {
        LOG.debug("Request to create department : {}", attributes);
        return objectMapper.valueToTree(secureDataManager.save(ENTITY_CODE, null, toAttributeMap(attributes), DETAIL_FETCH_PLAN));
    }

    public JsonNode update(Long id, JsonNode attributes) {
        LOG.debug("Request to update department : {}", id);
        return objectMapper.valueToTree(secureDataManager.save(ENTITY_CODE, id, toAttributeMap(attributes), DETAIL_FETCH_PLAN));
    }

    public JsonNode patch(Long id, JsonNode attributes) {
        LOG.debug("Request to patch department : {}", id);
        return objectMapper.valueToTree(secureDataManager.save(ENTITY_CODE, id, toAttributeMap(attributes), DETAIL_FETCH_PLAN));
    }

    @Transactional(readOnly = true)
    public Page<JsonNode> query(String fetchPlanCode, Pageable pageable, Map<String, Object> filters) {
        LOG.debug("Request to query departments");
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
        LOG.debug("Request to delete department : {}", id);
        secureDataManager.delete(ENTITY_CODE, id);
    }

    private String resolveFetchPlanCode(String fetchPlanCode, String defaultFetchPlanCode) {
        return fetchPlanCode == null || fetchPlanCode.isBlank() ? defaultFetchPlanCode : fetchPlanCode;
    }

    private Map<String, Object> toAttributeMap(JsonNode attributes) {
        Map<String, Object> attributeMap = objectMapper.convertValue(attributes, ATTRIBUTE_MAP_TYPE);
        adaptOrganizationReference(attributeMap);
        return attributeMap;
    }

    private void adaptOrganizationReference(Map<String, Object> attributes) {
        if (!attributes.containsKey(ORGANIZATION_REFERENCE_FIELD)) {
            return;
        }

        Long organizationId = extractReferenceId(attributes.get(ORGANIZATION_REFERENCE_FIELD));
        if (organizationId == null) {
            throw new IllegalArgumentException("department.organization reference requires an id");
        }

        secureDataManager
            .loadOne(ORGANIZATION_ENTITY_CODE, organizationId, ORGANIZATION_REFERENCE_FETCH_PLAN)
            .orElseThrow(() -> new AccessDeniedException("Organization reference not found or not accessible: " + organizationId));

        Organization organization = organizationRepository
            .findById(organizationId)
            .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + organizationId));
        attributes.put(ORGANIZATION_REFERENCE_FIELD, organization);
    }

    private Long extractReferenceId(Object referenceValue) {
        if (referenceValue instanceof Number number) {
            return number.longValue();
        }
        if (referenceValue instanceof Map<?, ?> referenceMap) {
            Object idValue = referenceMap.get("id");
            if (idValue instanceof Number number) {
                return number.longValue();
            }
        }
        return null;
    }
}
