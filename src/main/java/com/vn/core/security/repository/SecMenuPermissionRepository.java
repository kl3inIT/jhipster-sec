package com.vn.core.security.repository;

import com.vn.core.security.domain.SecMenuPermission;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link SecMenuPermission}.
 */
@Repository
public interface SecMenuPermissionRepository extends JpaRepository<SecMenuPermission, Long> {
    List<SecMenuPermission> findAllByAppNameAndRoleIn(String appName, Collection<String> roles);

    List<SecMenuPermission> findAllByAppName(String appName);
}
