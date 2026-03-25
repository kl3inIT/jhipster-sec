package com.vn.core.security.repository;

import com.vn.core.security.domain.SecNavigationGrant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link SecNavigationGrant}.
 */
@Repository
public interface SecNavigationGrantRepository extends JpaRepository<SecNavigationGrant, Long> {
    List<SecNavigationGrant> findAllByAppNameAndAuthorityNameIn(String appName, Collection<String> authorityNames);

    List<SecNavigationGrant> findAllByAppName(String appName);
}
