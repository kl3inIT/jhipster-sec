package com.vn.core.service.security;

import com.vn.core.security.MergedSecurityService;
import com.vn.core.security.domain.SecMenuPermission;
import com.vn.core.security.repository.SecMenuPermissionRepository;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Resolves app-scoped menu permissions for the current authenticated user.
 */
@Service
public class CurrentUserMenuPermissionService {

    private final MergedSecurityService mergedSecurityService;
    private final SecMenuPermissionRepository secMenuPermissionRepository;

    public CurrentUserMenuPermissionService(
        MergedSecurityService mergedSecurityService,
        SecMenuPermissionRepository secMenuPermissionRepository
    ) {
        this.mergedSecurityService = mergedSecurityService;
        this.secMenuPermissionRepository = secMenuPermissionRepository;
    }

    public List<String> getAllowedMenuIds(String appName) {
        Collection<String> authorityNames = mergedSecurityService.getCurrentUserAuthorityNames();
        if (authorityNames.isEmpty()) {
            return List.of();
        }

        List<SecMenuPermission> grants = secMenuPermissionRepository.findAllByAppNameAndRoleIn(appName, authorityNames);
        Set<String> deniedMenuIds = grants
            .stream()
            .filter(grant -> "DENY".equals(grant.getEffect()))
            .map(SecMenuPermission::getMenuId)
            .collect(java.util.stream.Collectors.toSet());

        return grants
            .stream()
            .filter(grant -> "ALLOW".equals(grant.getEffect()))
            .map(SecMenuPermission::getMenuId)
            .filter(menuId -> !deniedMenuIds.contains(menuId))
            .distinct()
            .sorted()
            .toList();
    }
}
