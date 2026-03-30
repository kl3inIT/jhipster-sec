package com.vn.core.service;

import com.vn.core.domain.Brand;
import com.vn.core.domain.Shoe;
import com.vn.core.repository.BrandRepository;
import com.vn.core.security.data.SecureDataManager;
import com.vn.core.security.data.SecureDataManager.EntityMutation;
import com.vn.core.security.data.SecuredLoadQuery;
import jakarta.persistence.EntityNotFoundException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Secured Shoe application service backed only by {@link SecureDataManager}.
 */
@Service
@Transactional
public class ShoeService {

    private static final Logger LOG = LoggerFactory.getLogger(ShoeService.class);
    private static final Class<Shoe> ENTITY_CLASS = Shoe.class;
    private static final String ENTITY_CODE = "shoe";
    private static final String LIST_FETCH_PLAN = "shoe-list";

    private final SecureDataManager secureDataManager;
    private final BrandRepository brandRepository;

    public ShoeService(SecureDataManager secureDataManager, BrandRepository brandRepository) {
        this.secureDataManager = secureDataManager;
        this.brandRepository = brandRepository;
    }

    @Transactional(readOnly = true)
    public Page<Shoe> list(Pageable pageable) {
        LOG.debug("Request to list shoes");
        return secureDataManager.loadList(ENTITY_CLASS, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Shoe> findOne(Long id) {
        LOG.debug("Request to get shoe : {}", id);
        return secureDataManager.loadOne(ENTITY_CLASS, id);
    }

    public Shoe create(EntityMutation<Shoe> mutation) {
        LOG.debug("Request to create shoe");
        return secureDataManager.save(ENTITY_CLASS, null, normalizeMutation(mutation));
    }

    public Shoe update(Long id, EntityMutation<Shoe> mutation) {
        LOG.debug("Request to update shoe : {}", id);
        return secureDataManager.save(ENTITY_CLASS, id, normalizeMutation(mutation));
    }

    public Shoe patch(Long id, EntityMutation<Shoe> mutation) {
        LOG.debug("Request to patch shoe : {}", id);
        return secureDataManager.save(ENTITY_CLASS, id, normalizeMutation(mutation));
    }

    @Transactional(readOnly = true)
    public Page<Shoe> query(String fetchPlanCode, Pageable pageable, Map<String, Object> filters) {
        LOG.debug("Request to query shoes");
        SecuredLoadQuery query = new SecuredLoadQuery(
            ENTITY_CODE,
            null,
            filters,
            pageable,
            pageable.getSort(),
            resolveFetchPlanCode(fetchPlanCode, LIST_FETCH_PLAN)
        );
        return secureDataManager.loadByQuery(ENTITY_CLASS, query);
    }

    public void delete(Long id) {
        LOG.debug("Request to delete shoe : {}", id);
        secureDataManager.delete(ENTITY_CLASS, id);
    }

    private String resolveFetchPlanCode(String fetchPlanCode, String defaultFetchPlanCode) {
        return fetchPlanCode == null || fetchPlanCode.isBlank() ? defaultFetchPlanCode : fetchPlanCode;
    }

    private EntityMutation<Shoe> normalizeMutation(EntityMutation<Shoe> mutation) {
        Shoe shoe = requireEntity(mutation);
        adaptBrandReference(shoe, mutation.changedAttributes());
        return mutation;
    }

    private Shoe requireEntity(EntityMutation<Shoe> mutation) {
        if (mutation == null || mutation.entity() == null) {
            throw new IllegalArgumentException("Typed shoe mutation is required");
        }
        return mutation.entity();
    }

    private void adaptBrandReference(Shoe shoe, Collection<String> changedAttributes) {
        if (changedAttributes == null || !changedAttributes.contains("brand")) {
            return;
        }

        Brand requestedBrand = shoe.getBrand();
        Long brandId = requestedBrand != null ? requestedBrand.getId() : null;
        if (brandId == null) {
            throw new IllegalArgumentException("shoe.brand reference requires an id");
        }

        secureDataManager
            .loadOne(Brand.class, brandId)
            .orElseThrow(() -> new AccessDeniedException("Brand reference not found or not accessible: " + brandId));

        Brand brand = brandRepository.findById(brandId).orElseThrow(() -> new EntityNotFoundException("Brand not found: " + brandId));
        shoe.setBrand(brand);
    }
}
