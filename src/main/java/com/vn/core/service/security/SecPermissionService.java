package com.vn.core.service.security;

import com.vn.core.security.repository.SecPermissionRepository;
import java.util.Collection;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for {@code SecPermission} write paths that owns cache eviction
 * after every create, update, or delete operation per D-02 and D-03.
 *
 * <p>Cache eviction is the responsibility of this service layer so the resource
 * layer stays free of cache concerns.
 */
@Service
@Transactional
public class SecPermissionService {

    /** Name of the Hazelcast cache holding the permission matrix per authority set. */
    public static final String PERMISSION_MATRIX_CACHE = "sec-permission-matrix";

    private final SecPermissionRepository secPermissionRepository;
    private final CacheManager cacheManager;

    public SecPermissionService(SecPermissionRepository secPermissionRepository, CacheManager cacheManager) {
        this.secPermissionRepository = secPermissionRepository;
        this.cacheManager = cacheManager;
    }

    /**
     * Evicts the permission-matrix cache so the next request rebuilds the matrix
     * from the current database state.
     */
    public void evictPermissionCache() {
        var cache = cacheManager.getCache(PERMISSION_MATRIX_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * Deletes all permissions for the given authority name and evicts the cache.
     */
    public void deleteAllByAuthorityName(String authorityName) {
        secPermissionRepository.deleteByAuthorityName(authorityName);
        evictPermissionCache();
    }

    /**
     * Deletes the given set of permissions and evicts the cache.
     */
    public void deleteAll(Collection<?> permissions) {
        secPermissionRepository.deleteAll((Iterable) permissions);
        evictPermissionCache();
    }
}
