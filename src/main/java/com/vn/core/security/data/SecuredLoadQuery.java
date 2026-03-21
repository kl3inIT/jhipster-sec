package com.vn.core.security.data;

import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Query object carrying all parameters needed for a secured entity load.
 */
public record SecuredLoadQuery(
    String entityCode,
    String jpql,
    Map<String, Object> parameters,
    Pageable pageable,
    Sort sort,
    String fetchPlanCode
) {
    /**
     * Convenience factory for simple paginated catalog reads.
     */
    public static SecuredLoadQuery of(String entityCode, String fetchPlanCode, Pageable pageable) {
        return new SecuredLoadQuery(entityCode, null, Map.of(), pageable, pageable.getSort(), fetchPlanCode);
    }
}
