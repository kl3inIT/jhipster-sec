package com.vn.core.service.security;

import com.vn.core.security.domain.SecPermission;
import com.vn.core.security.permission.RequestPermissionSnapshot;
import com.vn.core.security.permission.TargetType;
import com.vn.core.security.repository.SecPermissionRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for {@link SecPermission} write operations.
 *
 * <p>This service owns all cache eviction for the cross-request permission-matrix cache.
 * Per D-02 and D-03, every {@code SecPermission} create, update, or delete must evict the
 * shared Hazelcast cache so the next HTTP request observes the updated permission state.
 * TTL expiry must NOT be relied on for correctness; only write-path eviction provides the
 * required freshness guarantee.
 *
 * <p>This is the single eviction seam. {@link com.vn.core.web.rest.admin.security.SecPermissionAdminResource}
 * delegates all persistence and eviction work to this service and remains HTTP transport only,
 * consistent with CLAUDE.md layering rules.
 */
@Service
@Transactional
public class SecPermissionService {

    private static final Logger LOG = LoggerFactory.getLogger(SecPermissionService.class);

    private final SecPermissionRepository secPermissionRepository;

    public SecPermissionService(SecPermissionRepository secPermissionRepository) {
        this.secPermissionRepository = secPermissionRepository;
    }

    /**
     * Saves a new {@link SecPermission} and evicts the shared permission-matrix cache.
     *
     * @param entity the permission to persist (must have no id set)
     * @return the saved entity
     */
    @CacheEvict(cacheNames = RequestPermissionSnapshot.PERMISSION_MATRIX_CACHE, allEntries = true)
    public SecPermission save(SecPermission entity) {
        LOG.debug("Saving SecPermission and evicting permission cache: {}", entity);
        return secPermissionRepository.save(entity);
    }

    /**
     * Updates an existing {@link SecPermission} and evicts the shared permission-matrix cache.
     *
     * @param entity the permission to update (must have an id set)
     * @return the saved entity
     */
    @CacheEvict(cacheNames = RequestPermissionSnapshot.PERMISSION_MATRIX_CACHE, allEntries = true)
    public SecPermission update(SecPermission entity) {
        LOG.debug("Updating SecPermission and evicting permission cache: {}", entity);
        return secPermissionRepository.save(entity);
    }

    /**
     * Deletes all given {@link SecPermission} instances and evicts the shared permission-matrix cache.
     *
     * @param entities the permissions to delete
     */
    @CacheEvict(cacheNames = RequestPermissionSnapshot.PERMISSION_MATRIX_CACHE, allEntries = true)
    public void deleteAll(Collection<SecPermission> entities) {
        LOG.debug("Deleting {} SecPermission(s) and evicting permission cache", entities.size());
        secPermissionRepository.deleteAll(entities);
    }

    /**
     * Deletes a single {@link SecPermission} by id (including all sibling duplicates for the
     * same authority+targetType+target+action key) and evicts the shared permission-matrix cache.
     *
     * @param id the id of the permission to delete
     */
    @CacheEvict(cacheNames = RequestPermissionSnapshot.PERMISSION_MATRIX_CACHE, allEntries = true)
    public void deleteById(Long id) {
        LOG.debug("Deleting SecPermission id={} and evicting permission cache", id);
        secPermissionRepository
            .findById(id)
            .ifPresent(permission ->
                secPermissionRepository.deleteAll(
                    secPermissionRepository.findAllByAuthorityNameAndTargetTypeAndTargetAndActionOrderByIdAsc(
                        permission.getAuthorityName(),
                        permission.getTargetType(),
                        permission.getTarget(),
                        permission.getAction()
                    )
                )
            );
    }

    /**
     * Deletes all {@link SecPermission} entries for a given authority name and evicts the cache.
     *
     * @param authorityName the authority name whose permissions should be removed
     */
    @CacheEvict(cacheNames = RequestPermissionSnapshot.PERMISSION_MATRIX_CACHE, allEntries = true)
    public void deleteAllByAuthorityName(String authorityName) {
        LOG.debug("Deleting all SecPermissions for authority={} and evicting permission cache", authorityName);
        secPermissionRepository.deleteByAuthorityName(authorityName);
    }

    /**
     * Finds a {@link SecPermission} by id.
     *
     * @param id the id to find
     * @return an {@link Optional} with the result, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<SecPermission> findById(Long id) {
        return secPermissionRepository.findById(id);
    }

    /**
     * Returns all permissions filtered by duplicate-detection criteria.
     *
     * @param authorityName the authority
     * @param targetType    the target type
     * @param target        the target
     * @param action        the action
     * @return ordered list of existing duplicate entries
     */
    @Transactional(readOnly = true)
    public List<SecPermission> findDuplicates(String authorityName, TargetType targetType, String target, String action) {
        return secPermissionRepository.findAllByAuthorityNameAndTargetTypeAndTargetAndActionOrderByIdAsc(
            authorityName,
            targetType,
            target,
            action
        );
    }

    /**
     * Evicts the full permission-matrix cache without performing any write.
     * Should be called when an external operation (e.g. bulk import) modifies permissions
     * outside the normal service write paths.
     */
    @CacheEvict(cacheNames = RequestPermissionSnapshot.PERMISSION_MATRIX_CACHE, allEntries = true)
    public void evictPermissionCache() {
        LOG.debug("Explicitly evicting permission-matrix cache");
    }
}
