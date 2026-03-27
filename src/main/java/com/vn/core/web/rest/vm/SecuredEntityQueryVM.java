package com.vn.core.web.rest.vm;

import java.util.List;
import java.util.Map;

/**
 * JSON request contract for secured entity query endpoints.
 */
public record SecuredEntityQueryVM(
    String fetchPlanCode,
    Integer page,
    Integer size,
    List<String> sort,
    Map<String, Object> filters
) {}
