package com.vn.core.security.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.core.security.data.SecureDataManager;
import com.vn.core.security.fetch.FetchPlan;
import com.vn.core.security.fetch.FetchPlanResolver;
import com.vn.core.security.serialize.SecureEntitySerializer;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Explicit JSON edge adapter for secured entity endpoints.
 *
 * <p>Controllers can bind HTTP JSON into typed entity mutations here while the
 * internal secured-data flow continues on real entity instances.
 */
@Component
public class SecuredEntityJsonAdapter {

    private final ObjectMapper objectMapper;
    private final FetchPlanResolver fetchPlanResolver;
    private final SecureEntitySerializer secureEntitySerializer;
    private final SecuredEntityPayloadValidator securedEntityPayloadValidator;

    public SecuredEntityJsonAdapter(
        ObjectMapper objectMapper,
        FetchPlanResolver fetchPlanResolver,
        SecureEntitySerializer secureEntitySerializer,
        SecuredEntityPayloadValidator securedEntityPayloadValidator
    ) {
        this.objectMapper = objectMapper;
        this.fetchPlanResolver = fetchPlanResolver;
        this.secureEntitySerializer = secureEntitySerializer;
        this.securedEntityPayloadValidator = securedEntityPayloadValidator;
    }

    public <E> SecureDataManager.EntityMutation<E> fromJson(String body, Class<E> entityClass) {
        try {
            return fromJson(securedEntityPayloadValidator.validateMutationPayload(objectMapper.readTree(body), entityClass), entityClass);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body must be valid JSON", ex);
        }
    }

    public <E> SecureDataManager.EntityMutation<E> fromJson(JsonNode body, Class<E> entityClass) {
        JsonNode objectBody = securedEntityPayloadValidator.validateMutationPayload(body, entityClass);
        try {
            return new SecureDataManager.EntityMutation<>(
                objectMapper.convertValue(objectBody, entityClass),
                collectTopLevelFields(objectBody)
            );
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Request body could not be bound to " + entityClass.getSimpleName(),
                ex
            );
        }
    }

    public JsonNode toJson(Object entity, String fetchPlanCode) {
        if (entity == null) {
            return objectMapper.nullNode();
        }

        // D-08: resolve the fetch plan once per detail response call.
        FetchPlan fetchPlan = fetchPlanResolver.resolve(ClassUtils.getUserClass(entity), fetchPlanCode);
        return toJsonWithPlan(entity, fetchPlan);
    }

    public String toJsonString(Object entity, String fetchPlanCode) {
        try {
            return objectMapper.writeValueAsString(toJson(entity, fetchPlanCode));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize secured entity response", ex);
        }
    }

    public String toJsonArrayString(Iterable<?> entities, String fetchPlanCode) {
        if (entities == null) {
            try {
                return objectMapper.writeValueAsString(objectMapper.createArrayNode());
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Failed to serialize empty secured entity collection response", ex);
            }
        }

        try {
            java.util.List<?> entityList = java.util.stream.StreamSupport.stream(entities.spliterator(), false).toList();
            if (entityList.isEmpty()) {
                return objectMapper.writeValueAsString(objectMapper.createArrayNode());
            }

            // D-08: resolve the fetch plan once per list serialization call, not once per entity.
            FetchPlan fetchPlan = fetchPlanResolver.resolve(ClassUtils.getUserClass(entityList.get(0)), fetchPlanCode);
            return objectMapper.writeValueAsString(
                objectMapper.valueToTree(entityList.stream().map(entity -> toJsonWithPlan(entity, fetchPlan)).toList())
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize secured entity collection response", ex);
        }
    }

    /**
     * Serialize a single entity using an already-resolved {@link FetchPlan}.
     * This is the internal hot path used by both detail and list serialization.
     */
    private JsonNode toJsonWithPlan(Object entity, FetchPlan fetchPlan) {
        return objectMapper.valueToTree(secureEntitySerializer.serialize(entity, fetchPlan));
    }

    private Set<String> collectTopLevelFields(JsonNode body) {
        Set<String> fields = new LinkedHashSet<>();
        body.fieldNames().forEachRemaining(fields::add);
        return fields;
    }
}
