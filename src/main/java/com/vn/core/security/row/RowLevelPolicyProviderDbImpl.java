package com.vn.core.security.row;

import com.vn.core.security.MergedSecurityService;
import com.vn.core.security.domain.SecRowPolicy;
import com.vn.core.security.permission.EntityOp;
import com.vn.core.security.repository.SecRowPolicyRepository;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Database-backed implementation of {@link RowLevelPolicyProvider}.
 * Loads row policies from {@link SecRowPolicyRepository} and converts them into
 * {@link RowPolicyDefinition} instances backed by JPA {@link Specification}s.
 *
 * <p>Fail-closed per D-13: unparseable or unsupported policy expressions throw
 * {@link AccessDeniedException} rather than silently skipping the constraint.
 */
@Service
public class RowLevelPolicyProviderDbImpl implements RowLevelPolicyProvider {

    private static final Logger LOG = LoggerFactory.getLogger(RowLevelPolicyProviderDbImpl.class);

    static final String CURRENT_USER_LOGIN_TOKEN = "CURRENT_USER_LOGIN";
    static final String CURRENT_USER_LOGIN_JPQL_TOKEN = "{CURRENT_USER_LOGIN}";

    private final SecRowPolicyRepository secRowPolicyRepository;
    private final MergedSecurityService mergedSecurityService;

    public RowLevelPolicyProviderDbImpl(SecRowPolicyRepository secRowPolicyRepository, MergedSecurityService mergedSecurityService) {
        this.secRowPolicyRepository = secRowPolicyRepository;
        this.mergedSecurityService = mergedSecurityService;
    }

    @Override
    public List<RowPolicyDefinition> getPolicies(String entityName, EntityOp operation) {
        List<SecRowPolicy> policies = secRowPolicyRepository.findByEntityNameAndOperation(entityName, operation.name());
        List<RowPolicyDefinition> result = new ArrayList<>();
        for (SecRowPolicy policy : policies) {
            result.add(convertToDefinition(policy));
        }
        return result;
    }

    private RowPolicyDefinition convertToDefinition(SecRowPolicy policy) {
        String policyType = policy.getPolicyType();
        String expression = policy.getExpression();

        if ("JAVA".equals(policyType)) {
            throw new AccessDeniedException("JAVA row policies not supported in Phase 3 — policy: " + policy.getCode());
        }

        if ("SPECIFICATION".equals(policyType)) {
            return parseSpecificationPolicy(policy, expression);
        }

        if ("JPQL".equals(policyType)) {
            return parseJpqlPolicy(policy, expression);
        }

        throw new AccessDeniedException(
            "Unsafe row policy: " + policy.getCode() + " — cannot parse expression (unsupported policyType: " + policyType + ")"
        );
    }

    /**
     * Parses SPECIFICATION expressions of the form: {@code field = CURRENT_USER_LOGIN}
     */
    private RowPolicyDefinition parseSpecificationPolicy(SecRowPolicy policy, String expression) {
        String trimmed = expression.trim();
        // Expected format: "field = CURRENT_USER_LOGIN"
        String[] parts = trimmed.split("\\s*=\\s*", 2);
        if (parts.length != 2) {
            throw new AccessDeniedException(
                "Unsafe row policy: " + policy.getCode() + " — cannot parse expression (expected 'field = TOKEN'): " + expression
            );
        }
        String field = parts[0].trim();
        String token = parts[1].trim();

        if (!CURRENT_USER_LOGIN_TOKEN.equals(token)) {
            throw new AccessDeniedException(
                "Unsafe row policy: " + policy.getCode() + " — unsupported token in SPECIFICATION expression: " + token
            );
        }

        String resolvedValue = mergedSecurityService
            .getCurrentUserLogin()
            .orElseThrow(() -> new AccessDeniedException("Row policy requires authenticated user: " + policy.getCode()));

        LOG.debug("Building SPECIFICATION row policy for field={} value={}", field, resolvedValue);

        return new RowPolicyDefinition() {
            @Override
            public <T> Specification<T> getSpecification() {
                return (root, query, cb) -> cb.equal(root.get(field), resolvedValue);
            }
        };
    }

    /**
     * Parses JPQL WHERE fragment expressions.
     * Supported simple pattern: {@code e.field = :currentUserLogin} or {@code field = :currentUserLogin}
     * Token substitution: {@code {CURRENT_USER_LOGIN}} is replaced with the current user login.
     */
    private RowPolicyDefinition parseJpqlPolicy(SecRowPolicy policy, String expression) {
        // Replace JPQL token with resolved value
        String trimmed = expression.trim();

        // Resolve current user login for any token substitution
        String resolvedLogin = mergedSecurityService
            .getCurrentUserLogin()
            .orElseThrow(() -> new AccessDeniedException("Row policy requires authenticated user: " + policy.getCode()));

        // Replace {CURRENT_USER_LOGIN} token
        String resolved = trimmed.replace(CURRENT_USER_LOGIN_JPQL_TOKEN, resolvedLogin);

        // Support simple equality: "e.field = :value" or "field = :value" after token replacement
        // Pattern: optionalAlias.field = value
        // We extract field and compare as JPA criteria
        String cleanExpr = resolved.trim();
        // Remove alias prefix like "e." if present
        if (cleanExpr.contains(".")) {
            int dotIdx = cleanExpr.indexOf('.');
            // Check if the part before the dot looks like an alias (no spaces)
            String beforeDot = cleanExpr.substring(0, dotIdx).trim();
            if (!beforeDot.contains(" ")) {
                cleanExpr = cleanExpr.substring(dotIdx + 1);
            }
        }

        String[] parts = cleanExpr.split("\\s*=\\s*", 2);
        if (parts.length != 2) {
            throw new AccessDeniedException(
                "Unsafe row policy: " + policy.getCode() + " — cannot parse JPQL expression (expected 'field = value'): " + expression
            );
        }

        String field = parts[0].trim();
        String value = parts[1].trim();

        // Remove any leading colon from named parameter values (e.g. :currentUserLogin -> already resolved)
        if (value.startsWith(":")) {
            // Named parameter not substituted — complex pattern not supported
            throw new AccessDeniedException(
                "Unsafe row policy: " + policy.getCode() + " — unsupported JPQL named parameter pattern: " + expression
            );
        }

        LOG.debug("Building JPQL row policy for field={} value={}", field, value);
        final String resolvedValue = value;

        return new RowPolicyDefinition() {
            @Override
            public <T> Specification<T> getSpecification() {
                return (root, query, cb) -> cb.equal(root.get(field), resolvedValue);
            }
        };
    }
}
