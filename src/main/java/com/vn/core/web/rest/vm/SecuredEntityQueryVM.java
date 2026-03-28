package com.vn.core.web.rest.vm;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * JSON request contract for secured entity query endpoints.
 */
public record SecuredEntityQueryVM(
    @Size(max = 100) String fetchPlanCode,
    @PositiveOrZero Integer page,
    @Positive Integer size,
    List<String> sort,
    Map<String, Object> filters
) {}
