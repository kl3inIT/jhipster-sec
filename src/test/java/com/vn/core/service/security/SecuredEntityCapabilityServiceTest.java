package com.vn.core.service.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vn.core.security.MergedSecurityService;
import com.vn.core.security.catalog.SecuredEntityCatalog;
import com.vn.core.security.catalog.SecuredEntityEntry;
import com.vn.core.security.domain.SecPermission;
import com.vn.core.security.permission.EntityOp;
import com.vn.core.security.permission.TargetType;
import com.vn.core.security.repository.SecPermissionRepository;
import com.vn.core.service.dto.security.SecuredAttributeCapabilityDTO;
import com.vn.core.service.dto.security.SecuredEntityCapabilityDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SecuredEntityCapabilityServiceTest {

    @Mock
    private SecuredEntityCatalog securedEntityCatalog;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Metamodel metamodel;

    @Mock
    private MergedSecurityService mergedSecurityService;

    @Mock
    private SecPermissionRepository secPermissionRepository;

    private SecuredEntityCapabilityService securedEntityCapabilityService;

    @BeforeEach
    void setUp() {
        securedEntityCapabilityService = new SecuredEntityCapabilityService(
            securedEntityCatalog,
            entityManager,
            mergedSecurityService,
            secPermissionRepository
        );
        when(entityManager.getMetamodel()).thenReturn(metamodel);
    }

    @Test
    void returnsSortedCapabilitiesUsingUnionOfAllowForEntityAndWildcardAttributes() {
        when(mergedSecurityService.getCurrentUserAuthorityNames()).thenReturn(List.of("ROLE_USER", "ROLE_MANAGER"));
        when(securedEntityCatalog.entries()).thenReturn(List.of(entry(BetaEntity.class, "BETA"), entry(AlphaEntity.class, "ALPHA")));
        mockAttributes(AlphaEntity.class, "secret", "name");
        mockAttributes(BetaEntity.class, "title");
        when(secPermissionRepository.findAllByAuthorityNameIn(List.of("ROLE_USER", "ROLE_MANAGER"))).thenReturn(
            List.of(
                permission("ROLE_USER", TargetType.ENTITY, "ALPHAENTITY", EntityOp.READ.name(), "ALLOW"),
                permission("ROLE_MANAGER", TargetType.ENTITY, "ALPHAENTITY", EntityOp.READ.name(), "DENY"),
                permission("ROLE_USER", TargetType.ATTRIBUTE, "ALPHAENTITY.NAME", "VIEW", "ALLOW"),
                permission("ROLE_MANAGER", TargetType.ATTRIBUTE, "ALPHAENTITY.*", "EDIT", "ALLOW"),
                permission("ROLE_MANAGER", TargetType.ENTITY, "BETAENTITY", EntityOp.READ.name(), "DENY")
            )
        );

        List<SecuredEntityCapabilityDTO> result = securedEntityCapabilityService.getCurrentUserCapabilities();

        assertThat(result).extracting(SecuredEntityCapabilityDTO::getCode).containsExactly("ALPHA", "BETA");

        SecuredEntityCapabilityDTO alpha = capabilityByCode(result, "ALPHA");
        assertThat(alpha.isCanRead()).isTrue();
        assertThat(alpha.isCanCreate()).isFalse();
        assertThat(alpha.isCanUpdate()).isFalse();
        assertThat(alpha.isCanDelete()).isFalse();

        SecuredAttributeCapabilityDTO alphaName = attributeByName(alpha, "name");
        assertThat(alphaName.isCanView()).isTrue();
        assertThat(alphaName.isCanEdit()).isTrue();

        // ALPHAENTITY.*:EDIT wildcard implies VIEW for all attributes, including "secret"
        SecuredAttributeCapabilityDTO alphaSecret = attributeByName(alpha, "secret");
        assertThat(alphaSecret.isCanView()).isTrue();
        assertThat(alphaSecret.isCanEdit()).isTrue();

        SecuredEntityCapabilityDTO beta = capabilityByCode(result, "BETA");
        assertThat(beta.isCanRead()).isFalse();
        assertThat(attributeByName(beta, "title").isCanView()).isFalse();
        assertThat(attributeByName(beta, "title").isCanEdit()).isFalse();
    }

    @Test
    void deniesCapabilitiesWhenPermissionSetIsEmpty() {
        when(mergedSecurityService.getCurrentUserAuthorityNames()).thenReturn(List.of("ROLE_USER"));
        when(securedEntityCatalog.entries()).thenReturn(List.of(entry(AlphaEntity.class, "ALPHA")));
        mockAttributes(AlphaEntity.class, "name");
        when(secPermissionRepository.findAllByAuthorityNameIn(List.of("ROLE_USER"))).thenReturn(List.of());

        List<SecuredEntityCapabilityDTO> result = securedEntityCapabilityService.getCurrentUserCapabilities();

        SecuredEntityCapabilityDTO alpha = capabilityByCode(result, "ALPHA");
        assertThat(alpha.isCanRead()).isFalse();
        assertThat(alpha.isCanCreate()).isFalse();
        assertThat(alpha.isCanUpdate()).isFalse();
        assertThat(alpha.isCanDelete()).isFalse();
        assertThat(attributeByName(alpha, "name").isCanView()).isFalse();
        assertThat(attributeByName(alpha, "name").isCanEdit()).isFalse();
    }

    @Test
    void usesEmptyMatrixWhenCurrentUserHasNoAuthorities() {
        when(mergedSecurityService.getCurrentUserAuthorityNames()).thenReturn(List.of());
        when(securedEntityCatalog.entries()).thenReturn(List.of(entry(AlphaEntity.class, "ALPHA")));
        mockAttributes(AlphaEntity.class, "name");

        List<SecuredEntityCapabilityDTO> result = securedEntityCapabilityService.getCurrentUserCapabilities();

        SecuredEntityCapabilityDTO alpha = capabilityByCode(result, "ALPHA");
        assertThat(alpha.isCanRead()).isFalse();
        assertThat(attributeByName(alpha, "name").isCanView()).isFalse();
        verifyNoInteractions(secPermissionRepository);
    }

    private SecuredEntityEntry entry(Class<?> entityClass, String code) {
        return SecuredEntityEntry.builder().entityClass(entityClass).code(code).operations(EnumSet.allOf(EntityOp.class)).build();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void mockAttributes(Class<?> entityClass, String... attributeNames) {
        EntityType entityType = org.mockito.Mockito.mock(EntityType.class);
        when(metamodel.entity(entityClass)).thenReturn(entityType);
        Set<Attribute<?, ?>> attributes = new LinkedHashSet<>();
        for (String attributeName : attributeNames) {
            Attribute attribute = org.mockito.Mockito.mock(Attribute.class);
            when(attribute.getName()).thenReturn(attributeName);
            attributes.add(attribute);
        }
        when(entityType.getAttributes()).thenReturn((Set) attributes);
    }

    private SecPermission permission(String authorityName, TargetType targetType, String target, String action, String effect) {
        return new SecPermission().authorityName(authorityName).targetType(targetType).target(target).action(action).effect(effect);
    }

    private SecuredEntityCapabilityDTO capabilityByCode(List<SecuredEntityCapabilityDTO> capabilities, String code) {
        return capabilities
            .stream()
            .filter(capability -> code.equals(capability.getCode()))
            .findFirst()
            .orElseThrow();
    }

    private SecuredAttributeCapabilityDTO attributeByName(SecuredEntityCapabilityDTO capability, String name) {
        return capability
            .getAttributes()
            .stream()
            .filter(attribute -> name.equals(attribute.getName()))
            .findFirst()
            .orElseThrow();
    }

    static class AlphaEntity {

        String name;
        String secret;
    }

    static class BetaEntity {

        String title;
    }
}
