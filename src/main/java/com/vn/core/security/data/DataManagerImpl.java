package com.vn.core.security.data;

import com.vn.core.security.access.AccessManager;
import com.vn.core.security.access.CrudEntityContext;
import com.vn.core.security.permission.EntityOp;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Internal secure-default data manager that centralizes CRUD checks while delegating
 * raw repository mechanics to the explicit unconstrained bypass.
 */
@Service
@Transactional
public class DataManagerImpl implements DataManager {

    private final AccessManager accessManager;
    private final UnconstrainedDataManager unconstrainedDataManager;

    public DataManagerImpl(
        AccessManager accessManager,
        @Qualifier("unconstrainedDataManagerImpl") UnconstrainedDataManager unconstrainedDataManager
    ) {
        this.accessManager = accessManager;
        this.unconstrainedDataManager = unconstrainedDataManager;
    }

    @Override
    public UnconstrainedDataManager unconstrained() {
        return unconstrainedDataManager;
    }

    @Override
    public void checkCrud(Class<?> entityClass, EntityOp op) {
        CrudEntityContext ctx = new CrudEntityContext(entityClass, op);
        accessManager.applyRegisteredConstraints(ctx);
        if (!ctx.isPermitted()) {
            throw new AccessDeniedException(op.name() + " denied on " + entityClass.getSimpleName());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public <T> Optional<T> loadOne(Class<T> entityClass, Specification<T> spec, EntityOp op) {
        checkCrud(entityClass, op);
        return unconstrainedDataManager.loadOne(entityClass, spec);
    }

    @Override
    @Transactional(readOnly = true)
    public <T> List<T> loadList(Class<T> entityClass, Specification<T> spec, EntityOp op) {
        checkCrud(entityClass, op);
        return unconstrainedDataManager.loadList(entityClass, spec);
    }

    @Override
    @Transactional(readOnly = true)
    public <T> Page<T> loadPage(Class<T> entityClass, Specification<T> spec, Pageable pageable, EntityOp op) {
        checkCrud(entityClass, op);
        return unconstrainedDataManager.loadPage(entityClass, spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public <T> T load(Class<T> entityClass, Object id) {
        return unconstrainedDataManager.load(entityClass, id);
    }

    @Override
    @Transactional(readOnly = true)
    public <T> List<T> loadAll(Class<T> entityClass) {
        return unconstrainedDataManager.loadAll(entityClass);
    }

    @Override
    @Transactional(readOnly = true)
    public <T> Optional<T> loadOne(Class<T> entityClass, Specification<T> spec) {
        return unconstrainedDataManager.loadOne(entityClass, spec);
    }

    @Override
    @Transactional(readOnly = true)
    public <T> List<T> loadList(Class<T> entityClass, Specification<T> spec) {
        return unconstrainedDataManager.loadList(entityClass, spec);
    }

    @Override
    @Transactional(readOnly = true)
    public <T> Page<T> loadPage(Class<T> entityClass, Specification<T> spec, Pageable pageable) {
        return unconstrainedDataManager.loadPage(entityClass, spec, pageable);
    }

    @Override
    public <T> T create(Class<T> entityClass) {
        return unconstrainedDataManager.create(entityClass);
    }

    @Override
    public <T> T save(T entity) {
        return unconstrainedDataManager.save(entity);
    }

    @Override
    public void delete(Object entity) {
        unconstrainedDataManager.delete(entity);
    }

    @Override
    public void delete(Class<?> entityClass, Object id) {
        unconstrainedDataManager.delete(entityClass, id);
    }
}
