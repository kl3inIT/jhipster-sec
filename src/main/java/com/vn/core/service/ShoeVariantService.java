package com.vn.core.service;

import com.vn.core.domain.Shoe;
import com.vn.core.domain.ShoeVariant;
import com.vn.core.repository.ShoeRepository;
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
 * Secured ShoeVariant application service backed only by {@link SecureDataManager}.
 */
@Service
@Transactional
public class ShoeVariantService {

    private static final Logger LOG = LoggerFactory.getLogger(ShoeVariantService.class);
    private static final Class<ShoeVariant> ENTITY_CLASS = ShoeVariant.class;
    private static final String ENTITY_CODE = "shoevariant";
    private static final String LIST_FETCH_PLAN = "shoevariant-list";

    private final SecureDataManager secureDataManager;
    private final ShoeRepository shoeRepository;

    public ShoeVariantService(SecureDataManager secureDataManager, ShoeRepository shoeRepository) {
        this.secureDataManager = secureDataManager;
        this.shoeRepository = shoeRepository;
    }

    @Transactional(readOnly = true)
    public Page<ShoeVariant> list(Pageable pageable) {
        LOG.debug("Request to list shoeVariants");
        return secureDataManager.loadList(ENTITY_CLASS, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<ShoeVariant> findOne(Long id) {
        LOG.debug("Request to get shoeVariant : {}", id);
        return secureDataManager.loadOne(ENTITY_CLASS, id);
    }

    public ShoeVariant create(EntityMutation<ShoeVariant> mutation) {
        LOG.debug("Request to create shoeVariant");
        return secureDataManager.save(ENTITY_CLASS, null, normalizeMutation(mutation));
    }

    public ShoeVariant update(Long id, EntityMutation<ShoeVariant> mutation) {
        LOG.debug("Request to update shoeVariant : {}", id);
        return secureDataManager.save(ENTITY_CLASS, id, normalizeMutation(mutation));
    }

    public ShoeVariant patch(Long id, EntityMutation<ShoeVariant> mutation) {
        LOG.debug("Request to patch shoeVariant : {}", id);
        return secureDataManager.save(ENTITY_CLASS, id, normalizeMutation(mutation));
    }

    @Transactional(readOnly = true)
    public Page<ShoeVariant> query(String fetchPlanCode, Pageable pageable, Map<String, Object> filters) {
        LOG.debug("Request to query shoeVariants");
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
        LOG.debug("Request to delete shoeVariant : {}", id);
        secureDataManager.delete(ENTITY_CLASS, id);
    }

    private String resolveFetchPlanCode(String fetchPlanCode, String defaultFetchPlanCode) {
        return fetchPlanCode == null || fetchPlanCode.isBlank() ? defaultFetchPlanCode : fetchPlanCode;
    }

    private EntityMutation<ShoeVariant> normalizeMutation(EntityMutation<ShoeVariant> mutation) {
        ShoeVariant shoeVariant = requireEntity(mutation);
        adaptShoeReference(shoeVariant, mutation.changedAttributes());
        return mutation;
    }

    private ShoeVariant requireEntity(EntityMutation<ShoeVariant> mutation) {
        if (mutation == null || mutation.entity() == null) {
            throw new IllegalArgumentException("Typed shoeVariant mutation is required");
        }
        return mutation.entity();
    }

    private void adaptShoeReference(ShoeVariant shoeVariant, Collection<String> changedAttributes) {
        if (changedAttributes == null || !changedAttributes.contains("shoe")) {
            return;
        }

        Shoe requestedShoe = shoeVariant.getShoe();
        Long shoeId = requestedShoe != null ? requestedShoe.getId() : null;
        if (shoeId == null) {
            throw new IllegalArgumentException("shoeVariant.shoe reference requires an id");
        }

        secureDataManager.loadOne(Shoe.class, shoeId).orElseThrow(() -> new AccessDeniedException("Shoe reference not found or not accessible: " + shoeId));

        Shoe shoe = shoeRepository.findById(shoeId).orElseThrow(() -> new EntityNotFoundException("Shoe not found: " + shoeId));
        shoeVariant.setShoe(shoe);
    }
}
