package com.vn.core.security.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.core.security.fetch.FetchPlanResolver;
import com.vn.core.web.rest.vm.SecuredEntityQueryVM;
import jakarta.persistence.Entity;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Shared fail-closed validator for secured-entity JSON payloads and query shapes.
 */
@Component
public class SecuredEntityPayloadValidator {

    private static final Pattern SORT_PATTERN = Pattern.compile("^([A-Za-z][A-Za-z0-9]*)(,(asc|desc))?$", Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final FetchPlanResolver fetchPlanResolver;

    public SecuredEntityPayloadValidator(ObjectMapper objectMapper, Validator validator, FetchPlanResolver fetchPlanResolver) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.fetchPlanResolver = fetchPlanResolver;
    }

    public <E> JsonNode validateMutationPayload(JsonNode body, Class<E> entityClass) {
        requireObjectBody(body, "Request body must be a JSON object");
        validateKnownFields(body, entityClass, entityClass.getSimpleName());

        body.fields().forEachRemaining(entry -> validateMutationField(entityClass, entry.getKey(), entry.getValue()));
        return body;
    }

    public void validateQuery(SecuredEntityQueryVM request, Class<?> entityClass, String fetchPlanCode) {
        if (request == null) {
            throw badRequest("Query request body is required");
        }

        validateFetchPlan(entityClass, fetchPlanCode);
        validateSort(entityClass, request.sort());
        validateFilters(entityClass, request.filters());
    }

    private void validateFetchPlan(Class<?> entityClass, String fetchPlanCode) {
        try {
            fetchPlanResolver.resolve(entityClass, fetchPlanCode);
        } catch (IllegalArgumentException ex) {
            throw badRequest(ex.getMessage());
        }
    }

    private void validateMutationField(Class<?> entityClass, String fieldName, JsonNode value) {
        PropertyDescriptor descriptor = requireProperty(entityClass, fieldName);
        Class<?> propertyType = descriptor.getPropertyType();

        if (isAssociation(propertyType)) {
            validateAssociationReference(fieldName, propertyType, value);
            return;
        }

        if (isCollectionLike(propertyType)) {
            throw badRequest("Collection field '" + fieldName + "' is not supported in secured payloads");
        }

        Object typedValue = convertValue(value, propertyType, "Invalid value for field '" + fieldName + "'");
        Set<ConstraintViolation<Object>> violations = validator.validateValue((Class<Object>) entityClass, fieldName, typedValue);
        if (!violations.isEmpty()) {
            throw badRequest(firstViolationMessage(violations));
        }
    }

    private void validateAssociationReference(String fieldName, Class<?> propertyType, JsonNode value) {
        JsonNode objectValue = requireObjectBody(value, "Association field '" + fieldName + "' must be a JSON object");
        validateKnownFields(objectValue, propertyType, propertyType.getSimpleName());

        JsonNode idNode = objectValue.get("id");
        if (idNode == null || idNode.isNull() || (idNode.isTextual() && idNode.asText().isBlank())) {
            throw badRequest("Association field '" + fieldName + "' requires a non-null id");
        }
    }

    private void validateSort(Class<?> entityClass, java.util.List<String> sortValues) {
        if (sortValues == null) {
            return;
        }

        for (String sortValue : sortValues) {
            if (sortValue == null || sortValue.isBlank()) {
                throw badRequest("Sort values must not be blank");
            }

            Matcher matcher = SORT_PATTERN.matcher(sortValue.trim());
            if (!matcher.matches()) {
                throw badRequest("Invalid sort expression: " + sortValue);
            }

            String property = matcher.group(1);
            if (!isQueryableProperty(entityClass, property)) {
                throw badRequest("Sort field '" + property + "' is not allowed for " + entityClass.getSimpleName());
            }
        }
    }

    private void validateFilters(Class<?> entityClass, Map<String, Object> filters) {
        if (filters == null) {
            return;
        }

        filters.forEach((fieldName, value) -> {
            if (!isQueryableProperty(entityClass, fieldName)) {
                throw badRequest("Filter field '" + fieldName + "' is not allowed for " + entityClass.getSimpleName());
            }
            if (value == null) {
                throw badRequest("Filter field '" + fieldName + "' requires a non-null scalar value");
            }
            if (value instanceof Map<?, ?> || value instanceof Collection<?> || value.getClass().isArray()) {
                throw badRequest("Filter field '" + fieldName + "' must use a scalar value");
            }

            Class<?> propertyType = requireProperty(entityClass, fieldName).getPropertyType();
            Object typedValue = convertValue(value, propertyType, "Invalid filter value for field '" + fieldName + "'");
            Set<ConstraintViolation<Object>> violations = validator.validateValue((Class<Object>) entityClass, fieldName, typedValue);
            if (!violations.isEmpty()) {
                throw badRequest(firstViolationMessage(violations));
            }
        });
    }

    private void validateKnownFields(JsonNode body, Class<?> entityClass, String contextName) {
        body
            .fieldNames()
            .forEachRemaining(fieldName -> {
                if ("class".equals(fieldName)) {
                    throw badRequest("Unknown field 'class' for " + contextName);
                }
                requireProperty(entityClass, fieldName);
            });
    }

    private boolean isQueryableProperty(Class<?> entityClass, String fieldName) {
        PropertyDescriptor descriptor = BeanUtils.getPropertyDescriptor(entityClass, fieldName);
        if (descriptor == null || descriptor.getPropertyType() == null) {
            return false;
        }

        Class<?> propertyType = descriptor.getPropertyType();
        return !isAssociation(propertyType) && !isCollectionLike(propertyType);
    }

    private PropertyDescriptor requireProperty(Class<?> entityClass, String fieldName) {
        PropertyDescriptor descriptor = BeanUtils.getPropertyDescriptor(entityClass, fieldName);
        if (descriptor == null || descriptor.getPropertyType() == null) {
            throw badRequest("Unknown field '" + fieldName + "' for " + entityClass.getSimpleName());
        }
        return descriptor;
    }

    private JsonNode requireObjectBody(JsonNode body, String message) {
        if (body == null || !body.isObject()) {
            throw badRequest(message);
        }
        return body;
    }

    private Object convertValue(Object value, Class<?> targetType, String message) {
        try {
            return objectMapper.convertValue(value, targetType);
        } catch (IllegalArgumentException ex) {
            throw badRequest(message);
        }
    }

    @SuppressWarnings("unchecked")
    private String firstViolationMessage(Set<ConstraintViolation<Object>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage).findFirst().orElse("Invalid secured entity payload");
    }

    private boolean isAssociation(Class<?> propertyType) {
        return propertyType.isAnnotationPresent(Entity.class);
    }

    private boolean isCollectionLike(Class<?> propertyType) {
        return Collection.class.isAssignableFrom(propertyType) || Map.class.isAssignableFrom(propertyType);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
