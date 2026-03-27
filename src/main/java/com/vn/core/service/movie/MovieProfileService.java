package com.vn.core.service.movie;

import com.vn.core.domain.movie.enumeration.ekip.ProductionRole;
import com.vn.core.domain.movie.enumeration.Classification;
import com.vn.core.domain.movie.enumeration.Genre;
import com.vn.core.domain.movie.enumeration.MovieType;
import com.vn.core.domain.movie.enumeration.Status;
import com.vn.core.security.data.SecureDataManager;
import com.vn.core.security.data.SecuredLoadQuery;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MovieProfileService {

    private static final Logger LOG = LoggerFactory.getLogger(MovieProfileService.class);

    private static final String ENTITY_CODE = "movie-profile";
    private static final String LIST_FETCH_PLAN = "movie-profile-list";
    private static final String DETAIL_FETCH_PLAN = "movie-profile-detail";
    private static final String EKIP_ENTITY_CODE = "production-ekip";
    private static final String EKIP_LIST_FETCH_PLAN = "production-ekip-list";
    private static final String PROFILE_CODE_PREFIX = "FP-";
    private static final int BATCH_SIZE = 200;

    private final SecureDataManager secureDataManager;

    public MovieProfileService(SecureDataManager secureDataManager) {
        this.secureDataManager = secureDataManager;
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> list(Pageable pageable) {
        LOG.debug("Request to list movie profiles");
        return secureDataManager.loadList(ENTITY_CODE, LIST_FETCH_PLAN, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> findOne(Long id) {
        LOG.debug("Request to get movie profile : {}", id);
        return secureDataManager.loadOne(ENTITY_CODE, id, DETAIL_FETCH_PLAN);
    }

    public Map<String, Object> create(Map<String, Object> attributes) {
        LOG.debug("Request to create movie profile : {}", attributes);
        List<Map<String, Object>> ekipMembers = readEkipMembers(attributes);
        Map<String, Object> saved = secureDataManager.save(ENTITY_CODE, null, sanitizeMovieProfileAttributes(attributes), DETAIL_FETCH_PLAN);
        Long savedId = readLong(saved, "id");
        if (savedId != null) {
            syncProductionEkips(savedId, ekipMembers);
            return secureDataManager.loadOne(ENTITY_CODE, savedId, DETAIL_FETCH_PLAN).orElse(saved);
        }
        return saved;
    }

    public Map<String, Object> update(Long id, Map<String, Object> attributes) {
        LOG.debug("Request to update movie profile : {}", id);
        List<Map<String, Object>> ekipMembers = readEkipMembers(attributes);
        Map<String, Object> saved = secureDataManager.save(ENTITY_CODE, id, sanitizeMovieProfileAttributes(attributes), DETAIL_FETCH_PLAN);
        syncProductionEkips(id, ekipMembers);
        return secureDataManager.loadOne(ENTITY_CODE, id, DETAIL_FETCH_PLAN).orElse(saved);
    }

    public void delete(Long id) {
        LOG.debug("Request to delete movie profile : {}", id);
        secureDataManager.delete(ENTITY_CODE, id);
    }

    @Transactional(readOnly = true)
    public long count() {
        Page<Map<String, Object>> page = secureDataManager.loadList(ENTITY_CODE, LIST_FETCH_PLAN, PageRequest.of(0, 1));
        return page.getTotalElements();
    }

    @Transactional(readOnly = true)
    public String nextCode(Integer year) {
        int safeYear = year != null ? year : 0;
        String prefix = PROFILE_CODE_PREFIX + safeYear + "-";
        int maxSeq = 0;
        for (Map<String, Object> profile : loadAllMovieProfiles()) {
            Object productionYear = profile.get("productionYear");
            if (!(productionYear instanceof Number) || ((Number) productionYear).intValue() != safeYear) {
                continue;
            }
            String code = readString(profile, "profileCode");
            if (code == null || !code.startsWith(prefix)) {
                continue;
            }
            String seqText = code.substring(prefix.length());
            try {
                maxSeq = Math.max(maxSeq, Integer.parseInt(seqText));
            } catch (NumberFormatException ignored) {
                // Skip malformed sequence values.
            }
        }
        return prefix + String.format(Locale.ROOT, "%03d", maxSeq + 1);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listProductionEkips(Long movieProfileId) {
        return loadAllProductionEkips()
            .stream()
            .filter(item -> movieProfileId.equals(readNestedId(item, "movieProfile")))
            .map(this::toProductionEkipResponse)
            .toList();
    }

    public List<Map<String, Object>> replaceProductionEkips(Long movieProfileId, List<Map<String, Object>> attributes) {
        secureDataManager
            .loadOne(ENTITY_CODE, movieProfileId, DETAIL_FETCH_PLAN)
            .orElseThrow(() -> new IllegalArgumentException("Movie profile not found: " + movieProfileId));

        syncProductionEkips(movieProfileId, attributes);
        return listProductionEkips(movieProfileId);
    }

    private Map<String, Object> sanitizeMovieProfileAttributes(Map<String, Object> attributes) {
        Map<String, Object> sanitized = new LinkedHashMap<>(attributes);
        sanitized.remove("ekipMembers");
        sanitized.put("movieType", parseEnum(MovieType.class, sanitized.get("movieType")));
        sanitized.put("classification", parseEnum(Classification.class, sanitized.get("classification")));
        sanitized.put("genre", parseEnum(Genre.class, sanitized.get("genre")));
        sanitized.put("status", parseEnum(Status.class, sanitized.get("status")));
        sanitized.put("startDate", parseLocalDate(sanitized.get("startDate")));
        sanitized.put("endDate", parseLocalDate(sanitized.get("endDate")));
        return sanitized;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readEkipMembers(Map<String, Object> attributes) {
        Object raw = attributes.get("ekipMembers");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                result.add((Map<String, Object>) map);
            }
        }
        return result;
    }

    private void syncProductionEkips(Long movieProfileId, List<Map<String, Object>> attributes) {
        // Upsert ekip rows and delete removed ones in the same service transaction.
        List<Map<String, Object>> existing = listProductionEkips(movieProfileId);
        Set<Long> existingIds = new HashSet<>();
        for (Map<String, Object> row : existing) {
            Long id = readLong(row, "id");
            if (id != null) {
                existingIds.add(id);
            }
        }

        Set<Long> incomingIds = new HashSet<>();
        for (Map<String, Object> item : attributes) {
            Long ekipId = readLong(item, "id");
            if (ekipId != null) {
                incomingIds.add(ekipId);
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("ekipName", readString(item, "ekipName"));
            payload.put("role", parseEnum(ProductionRole.class, item.get("role")));
            payload.put("movieProfile.id", movieProfileId);
            secureDataManager.save(EKIP_ENTITY_CODE, ekipId, payload, EKIP_LIST_FETCH_PLAN);
        }

        for (Long ekipId : existingIds) {
            if (!incomingIds.contains(ekipId)) {
                secureDataManager.delete(EKIP_ENTITY_CODE, ekipId);
            }
        }
    }

    private LocalDate parseLocalDate(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof LocalDate localDate) {
            return localDate;
        }
        String text = String.valueOf(raw).trim();
        if (text.isEmpty() || "?".equals(text)) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format (expected yyyy-MM-dd): " + text, e);
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, Object raw) {
        if (raw == null) {
            return null;
        }
        if (enumType.isInstance(raw)) {
            return enumType.cast(raw);
        }
        String text = String.valueOf(raw).trim();
        if (text.isEmpty()) {
            return null;
        }
        return Enum.valueOf(enumType, text);
    }

    @SuppressWarnings("unchecked")
    private Long readNestedId(Map<String, Object> source, String key) {
        Object nested = source.get(key);
        if (nested instanceof Map<?, ?> nestedMap) {
            Object id = ((Map<String, Object>) nestedMap).get("id");
            if (id instanceof Number number) {
                return number.longValue();
            }
            if (id != null) {
                try {
                    return Long.parseLong(String.valueOf(id));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private Long readLong(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null) {
            try {
                return Long.parseLong(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Map<String, Object> toProductionEkipResponse(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", readLong(source, "id"));
        result.put("movieProfileId", readNestedId(source, "movieProfile"));
        result.put("ekipName", readString(source, "ekipName"));
        result.put("role", readString(source, "role"));
        return result;
    }

    private List<Map<String, Object>> loadAllMovieProfiles() {
        List<Map<String, Object>> allRows = new ArrayList<>();
        int pageNumber = 0;
        while (true) {
            PageRequest request = PageRequest.of(pageNumber, BATCH_SIZE, Sort.by(Sort.Direction.ASC, "id"));
            Page<Map<String, Object>> page = secureDataManager.loadByQuery(SecuredLoadQuery.of(ENTITY_CODE, LIST_FETCH_PLAN, request));
            allRows.addAll(page.getContent());
            if (!page.hasNext()) {
                break;
            }
            pageNumber++;
        }
        return allRows;
    }

    private List<Map<String, Object>> loadAllProductionEkips() {
        List<Map<String, Object>> allRows = new ArrayList<>();
        int pageNumber = 0;
        while (true) {
            PageRequest request = PageRequest.of(pageNumber, BATCH_SIZE, Sort.by(Sort.Direction.ASC, "id"));
            Page<Map<String, Object>> page = secureDataManager.loadByQuery(SecuredLoadQuery.of(EKIP_ENTITY_CODE, EKIP_LIST_FETCH_PLAN, request));
            allRows.addAll(page.getContent());
            if (!page.hasNext()) {
                break;
            }
            pageNumber++;
        }
        return allRows;
    }

    private String readString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : null;
    }
}
