package com.vn.core.security.merge;

import com.vn.core.security.permission.AttributePermissionEvaluator;
import java.util.Collection;
import java.util.Map;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Write-side attribute enforcement for entity merge operations.
 * Unauthorized attribute edits are rejected with {@link AccessDeniedException} (fail-closed per D-18).
 * Identity fields ({@code id}) are never writable through merge.
 */
@Component
public class SecureMergeServiceImpl implements SecureMergeService {

    private final AttributePermissionEvaluator attributePermissionEvaluator;

    public SecureMergeServiceImpl(AttributePermissionEvaluator attributePermissionEvaluator) {
        this.attributePermissionEvaluator = attributePermissionEvaluator;
    }

    @Override
    public void mergeForUpdate(Object entity, Map<String, Object> attributes) {
        Class<?> entityClass = entity.getClass();
        BeanWrapperImpl wrapper = new BeanWrapperImpl(entity);

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            String attr = entry.getKey();

            // Identity fields are never writable through merge
            if ("id".equals(attr)) {
                continue;
            }

            if (!attributePermissionEvaluator.canEdit(entityClass, attr)) {
                throw new AccessDeniedException("No EDIT permission for " + entityClass.getSimpleName() + "." + attr);
            }

            wrapper.setPropertyValue(attr, entry.getValue());
        }
    }

    @Override
    public void mergeForUpdate(Object entity, Object sourceEntity, Collection<String> changedAttributes) {
        if (sourceEntity == null || changedAttributes == null) {
            return;
        }

        Class<?> entityClass = entity.getClass();
        BeanWrapperImpl targetWrapper = new BeanWrapperImpl(entity);
        BeanWrapperImpl sourceWrapper = new BeanWrapperImpl(sourceEntity);

        for (String attr : changedAttributes) {
            if ("id".equals(attr)) {
                continue;
            }

            if (!attributePermissionEvaluator.canEdit(entityClass, attr)) {
                throw new AccessDeniedException("No EDIT permission for " + entityClass.getSimpleName() + "." + attr);
            }

            targetWrapper.setPropertyValue(attr, sourceWrapper.getPropertyValue(attr));
        }
    }
}
