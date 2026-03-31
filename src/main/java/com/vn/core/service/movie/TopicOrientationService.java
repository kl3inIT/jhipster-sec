package com.vn.core.service.movie;

import com.vn.core.domain.movie.TopicOrientation;
import com.vn.core.repository.movie.TopicOrientationRepository;
import com.vn.core.security.data.SecureDataManager;
import com.vn.core.security.data.SecureDataManager.EntityMutation;
import java.time.Year;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TopicOrientationService {

    private static final Logger LOG = LoggerFactory.getLogger(TopicOrientationService.class);
    private static final Class<TopicOrientation> TOPIC_ORIENTATION_CLASS = TopicOrientation.class;
    private static final String CODE_PREFIX = "TO";
    private static final String CODE_SEPARATOR = "-";

    private final SecureDataManager secureDataManager;
    private final TopicOrientationRepository topicOrientationRepository;

    public TopicOrientationService(SecureDataManager secureDataManager, TopicOrientationRepository topicOrientationRepository) {
        this.secureDataManager = secureDataManager;
        this.topicOrientationRepository = topicOrientationRepository;
    }

    @Transactional(readOnly = true)
    public Page<TopicOrientation> list(Pageable pageable) {
        LOG.debug("Request to list topic orientations");
        return secureDataManager.loadList(TOPIC_ORIENTATION_CLASS, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<TopicOrientation> findOne(Long id) {
        LOG.debug("Request to get topic orientation : {}", id);
        return secureDataManager.loadOne(TOPIC_ORIENTATION_CLASS, id);
    }

    public TopicOrientation create(EntityMutation<TopicOrientation> mutation) {
        LOG.debug("Request to create topic orientation");
        EntityMutation<TopicOrientation> normalized = normalizeForCreate(mutation);
        TopicOrientation saved = secureDataManager.save(TOPIC_ORIENTATION_CLASS, null, normalized);
        if (saved.getId() != null) {
            return secureDataManager.loadOne(TOPIC_ORIENTATION_CLASS, saved.getId()).orElse(saved);
        }
        return saved;
    }

    public TopicOrientation update(Long id, EntityMutation<TopicOrientation> mutation) {
        LOG.debug("Request to update topic orientation : {}", id);
        TopicOrientation saved = secureDataManager.save(TOPIC_ORIENTATION_CLASS, id, mutation);
        return secureDataManager.loadOne(TOPIC_ORIENTATION_CLASS, id).orElse(saved);
    }

    public void delete(Long id) {
        LOG.debug("Request to delete topic orientation : {}", id);
        secureDataManager.delete(TOPIC_ORIENTATION_CLASS, id);
    }

    private EntityMutation<TopicOrientation> normalizeForCreate(EntityMutation<TopicOrientation> mutation) {
        if (mutation == null || mutation.entity() == null) {
            throw new IllegalArgumentException("Topic orientation mutation is required");
        }

        TopicOrientation entity = mutation.entity();
        if (entity.getCode() == null || entity.getCode().isBlank()) {
            entity.setCode(nextCode());
        }

        Collection<String> changedAttributes = mutation.changedAttributes();
        Set<String> normalizedAttributes =
            changedAttributes == null ? new LinkedHashSet<>() : new LinkedHashSet<>(changedAttributes);
        normalizedAttributes.add("code");
        return new EntityMutation<>(entity, normalizedAttributes);
    }

    private String nextCode() {
        String year = String.valueOf(Year.now().getValue());
        String prefix = CODE_PREFIX + CODE_SEPARATOR + year + CODE_SEPARATOR;
        int nextNumber = 1;
        Optional<String> topCode = topicOrientationRepository.findTopCodeByPrefix(prefix);
        if (topCode.isPresent()) {
            String sequence = topCode.get().substring(prefix.length());
            try {
                nextNumber = Integer.parseInt(sequence) + 1;
            } catch (NumberFormatException ignored) {
                // Fallback to default sequence when existing code is malformed.
            }
        }
        return prefix + String.format("%04d", nextNumber);
    }

}
