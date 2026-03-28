package com.vn.core.security.repository;

import com.vn.core.security.domain.SecPermission;
import com.vn.core.security.permission.TargetType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link SecPermission}.
 */
@Repository
public interface SecPermissionRepository extends JpaRepository<SecPermission, Long> {
    List<SecPermission> findByAuthorityName(String authorityName);

    List<SecPermission> findAllByAuthorityNameAndTargetTypeAndTargetAndActionOrderByIdAsc(
        String authorityName,
        TargetType targetType,
        String target,
        String action
    );

    void deleteByAuthorityName(String authorityName);

    @Query(
        "select p from SecPermission p " +
            "where p.authorityName in :authorityNames " +
            "and p.targetType = :targetType " +
            "and p.target = :target " +
            "and p.action = :action"
    )
    List<SecPermission> findByRolesAndTarget(
        @Param("authorityNames") Collection<String> authorityNames,
        @Param("targetType") TargetType targetType,
        @Param("target") String target,
        @Param("action") String action
    );

    @Query(
        "select p from SecPermission p " +
            "where p.authorityName in :authorityNames " +
            "and p.targetType = :targetType " +
            "and p.target in :targets " +
            "and p.action = :action"
    )
    List<SecPermission> findByRolesAndTargets(
        @Param("authorityNames") Collection<String> authorityNames,
        @Param("targetType") TargetType targetType,
        @Param("targets") Collection<String> targets,
        @Param("action") String action
    );

    @Query("select p from SecPermission p where p.authorityName in :authorityNames")
    List<SecPermission> findAllByAuthorityNameIn(@Param("authorityNames") Collection<String> authorityNames);
}
