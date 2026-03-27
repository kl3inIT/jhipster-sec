package com.vn.core.security.data;

import com.vn.core.security.permission.EntityOp;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * Internal secure-default data access contract.
 *
 * <p>Application services keep using {@link SecureDataManager}. This interface exists for the
 * internal security data layer, where CRUD-checked access is the default and
 * {@link #unconstrained()} exposes the explicit bypass path.
 */
public interface DataManager extends UnconstrainedDataManager {
    UnconstrainedDataManager unconstrained();

    <T> Optional<T> loadOne(Class<T> entityClass, Specification<T> spec, EntityOp op);

    <T> List<T> loadList(Class<T> entityClass, Specification<T> spec, EntityOp op);

    <T> Page<T> loadPage(Class<T> entityClass, Specification<T> spec, Pageable pageable, EntityOp op);

    void checkCrud(Class<?> entityClass, EntityOp op);
}
