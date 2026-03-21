package com.vn.core.security.repository;

import com.vn.core.security.domain.SecRowPolicy;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link SecRowPolicy}.
 */
@Repository
public interface SecRowPolicyRepository extends JpaRepository<SecRowPolicy, Long> {
    Optional<SecRowPolicy> findByCode(String code);

    List<SecRowPolicy> findByEntityNameAndOperation(String entityName, String operation);
}
