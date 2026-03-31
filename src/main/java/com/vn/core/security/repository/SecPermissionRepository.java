package com.vn.core.security.repository;

import com.vn.core.security.domain.SecPermission;
import com.vn.core.security.permission.TargetType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /**
     * Deletes all specific (non-wildcard) entity permissions for a given authority+action+effect.
     * Called when an entity wildcard ({@code target='*'}) is saved to remove redundant specific rows.
     */
    @Modifying
    @Query(
        "delete from SecPermission p " +
            "where p.authorityName = :authorityName " +
            "and p.targetType = com.vn.core.security.permission.TargetType.ENTITY " +
            "and p.target != '*' " +
            "and p.action = :action " +
            "and p.effect = :effect"
    )
    void deleteSpecificEntityPermissions(
        @Param("authorityName") String authorityName,
        @Param("action") String action,
        @Param("effect") String effect
    );

    /**
     * Deletes all specific (non-wildcard) attribute permissions under a given entity prefix
     * for a given authority+action+effect.
     * Called when an attribute wildcard (e.g. {@code ORGANIZATION.*}) is saved.
     *
     * @param entityPrefix  LIKE pattern matching specific attributes, e.g. {@code ORGANIZATION.%}
     * @param wildcardTarget the wildcard row to keep, e.g. {@code ORGANIZATION.*}
     */
    @Modifying
    @Query(
        "delete from SecPermission p " +
            "where p.authorityName = :authorityName " +
            "and p.targetType = com.vn.core.security.permission.TargetType.ATTRIBUTE " +
            "and p.target like :entityPrefix " +
            "and p.target != :wildcardTarget " +
            "and p.action = :action " +
            "and p.effect = :effect"
    )
    void deleteSpecificAttributePermissions(
        @Param("authorityName") String authorityName,
        @Param("entityPrefix") String entityPrefix,
        @Param("wildcardTarget") String wildcardTarget,
        @Param("action") String action,
        @Param("effect") String effect
    );
}
