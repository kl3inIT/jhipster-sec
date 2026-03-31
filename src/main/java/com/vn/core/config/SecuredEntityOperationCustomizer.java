package com.vn.core.config;

import com.vn.core.web.rest.DepartmentResource;
import com.vn.core.web.rest.EmployeeResource;
import com.vn.core.web.rest.OrganizationResource;
import io.swagger.v3.oas.models.Operation;
import java.util.Set;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

/**
 * Adds the {@code x-secured-entity: true} OpenAPI extension to all operations
 * on controllers that use the @SecuredEntity pipeline.
 */
@Component
public class SecuredEntityOperationCustomizer implements OperationCustomizer {

    private static final Set<Class<?>> SECURED_ENTITY_CONTROLLERS = Set.of(
        OrganizationResource.class,
        DepartmentResource.class,
        EmployeeResource.class
    );

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        if (SECURED_ENTITY_CONTROLLERS.contains(handlerMethod.getBeanType())) {
            operation.addExtension("x-secured-entity", true);
        }
        return operation;
    }
}
