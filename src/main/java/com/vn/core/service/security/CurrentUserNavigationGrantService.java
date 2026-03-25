package com.vn.core.service.security;

import com.vn.core.security.MergedSecurityService;
import com.vn.core.security.domain.SecNavigationGrant;
import com.vn.core.security.repository.SecNavigationGrantRepository;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Resolves app-scoped navigation grants for the current authenticated user.
 */
@Service
public class CurrentUserNavigationGrantService {

    private final MergedSecurityService mergedSecurityService;
    private final SecNavigationGrantRepository secNavigationGrantRepository;

    public CurrentUserNavigationGrantService(
        MergedSecurityService mergedSecurityService,
        SecNavigationGrantRepository secNavigationGrantRepository
    ) {
        this.mergedSecurityService = mergedSecurityService;
        this.secNavigationGrantRepository = secNavigationGrantRepository;
    }

    public List<String> getAllowedNodeIds(String appName) {
        Collection<String> authorityNames = mergedSecurityService.getCurrentUserAuthorityNames();
        if (authorityNames.isEmpty()) {
            return List.of();
        }

        List<SecNavigationGrant> grants = secNavigationGrantRepository.findAllByAppNameAndAuthorityNameIn(appName, authorityNames);
        Set<String> deniedNodeIds = grants
            .stream()
            .filter(grant -> "DENY".equals(grant.getEffect()))
            .map(SecNavigationGrant::getNodeId)
            .collect(java.util.stream.Collectors.toSet());

        return grants
            .stream()
            .filter(grant -> "ALLOW".equals(grant.getEffect()))
            .map(SecNavigationGrant::getNodeId)
            .filter(nodeId -> !deniedNodeIds.contains(nodeId))
            .distinct()
            .sorted()
            .toList();
    }
}
