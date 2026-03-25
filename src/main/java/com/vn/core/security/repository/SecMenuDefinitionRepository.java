package com.vn.core.security.repository;

import com.vn.core.security.domain.SecMenuDefinition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link SecMenuDefinition}.
 */
@Repository
public interface SecMenuDefinitionRepository extends JpaRepository<SecMenuDefinition, Long> {
    List<SecMenuDefinition> findAllByAppName(String appName);

    Optional<SecMenuDefinition> findByAppNameAndMenuId(String appName, String menuId);
}
