package com.vn.core.service.movie;

import com.vn.core.domain.movie.MovieProfile;
import com.vn.core.domain.movie.ProductionEkip;
import com.vn.core.domain.movie.enumeration.ekip.ProductionRole;
import com.vn.core.repository.movie.MovieProfileRepository;
import com.vn.core.security.data.SecureDataManager;
import com.vn.core.security.data.SecureDataManager.EntityMutation;
import com.vn.core.security.data.SecuredLoadQuery;
import java.util.ArrayList;
import java.util.HashMap;
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

    private static final Class<MovieProfile> MOVIE_PROFILE_CLASS = MovieProfile.class;
    private static final Class<ProductionEkip> PRODUCTION_EKIP_CLASS = ProductionEkip.class;
    private static final String ENTITY_CODE = "movie-profile";
    private static final String EKIP_ENTITY_CODE = "production-ekip";
    private static final String PROFILE_CODE_PREFIX = "FP-";
    private static final int BATCH_SIZE = 200;

    private final SecureDataManager secureDataManager;
    private final MovieProfileRepository movieProfileRepository;

    public MovieProfileService(SecureDataManager secureDataManager, MovieProfileRepository movieProfileRepository) {
        this.secureDataManager = secureDataManager;
        this.movieProfileRepository = movieProfileRepository;
    }

    @Transactional(readOnly = true)
    public Page<MovieProfile> list(Pageable pageable) {
        LOG.debug("Request to list movie profiles");
        return secureDataManager.loadList(MOVIE_PROFILE_CLASS, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<MovieProfile> findOne(Long id) {
        LOG.debug("Request to get movie profile : {}", id);
        return secureDataManager.loadOne(MOVIE_PROFILE_CLASS, id);
    }

    public MovieProfile create(EntityMutation<MovieProfile> mutation, List<Map<String, Object>> ekipMembers) {
        LOG.debug("Request to create movie profile");
        MovieProfile saved = secureDataManager.save(MOVIE_PROFILE_CLASS, null, mutation);
        if (saved.getId() != null) {
            if (ekipMembers != null) {
                syncProductionEkips(saved.getId(), ekipMembers);
            }
            return secureDataManager.loadOne(MOVIE_PROFILE_CLASS, saved.getId()).orElse(saved);
        }
        return saved;
    }

    public MovieProfile update(Long id, EntityMutation<MovieProfile> mutation, List<Map<String, Object>> ekipMembers) {
        MovieProfile saved = secureDataManager.save(MOVIE_PROFILE_CLASS, id, mutation);
        if (ekipMembers != null) {
            syncProductionEkips(id, ekipMembers);
        }
        return secureDataManager.loadOne(MOVIE_PROFILE_CLASS, id).orElse(saved);
    }

    public void delete(Long id) {
        LOG.debug("Request to delete movie profile : {}", id);
        secureDataManager.delete(MOVIE_PROFILE_CLASS, id);
    }

    @Transactional(readOnly = true)
    public long count() {
        return secureDataManager.loadList(MOVIE_PROFILE_CLASS, PageRequest.of(0, 1)).getTotalElements();
    }

    @Transactional(readOnly = true)
    public String nextCode(Integer year) {
        int safeYear = year != null ? year : 0;
        String prefix = PROFILE_CODE_PREFIX + safeYear + "-";
        int maxSeq = 0;
        int pageNumber = 0;
        while (true) {
            PageRequest request = PageRequest.of(pageNumber, BATCH_SIZE, Sort.by(Sort.Direction.ASC, "id"));
            SecuredLoadQuery query = new SecuredLoadQuery(
                ENTITY_CODE,
                null,
                Map.of("productionYear", safeYear),
                request,
                null,
                "movie-profile-list"
            );
            Page<MovieProfile> page = secureDataManager.loadByQuery(MOVIE_PROFILE_CLASS, query);
            for (MovieProfile profile : page.getContent()) {
                String code = profile.getProfileCode();
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
            if (!page.hasNext()) {
                break;
            }
            pageNumber++;
        }
        return prefix + String.format(Locale.ROOT, "%03d", maxSeq + 1);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listProductionEkips(Long movieProfileId) {
        return loadProductionEkipsForProfile(movieProfileId)
            .stream()
            .map(ekip -> toProductionEkipResponse(ekip, movieProfileId))
            .toList();
    }

    public List<Map<String, Object>> replaceProductionEkips(Long movieProfileId, List<Map<String, Object>> attributes) {
        secureDataManager
            .loadOne(MOVIE_PROFILE_CLASS, movieProfileId)
            .orElseThrow(() -> new IllegalArgumentException("Movie profile not found: " + movieProfileId));

        syncProductionEkips(movieProfileId, attributes);
        return listProductionEkips(movieProfileId);
    }

    private void syncProductionEkips(Long movieProfileId, List<Map<String, Object>> attributes) {
        MovieProfile managedProfile = movieProfileRepository
            .findById(movieProfileId)
            .orElseThrow(() -> new IllegalArgumentException("Movie profile not found: " + movieProfileId));
        List<ProductionEkip> existing = new ArrayList<>(managedProfile.getEkipMembers());
        Map<Long, ProductionEkip> existingById = new HashMap<>();
        for (ProductionEkip row : existing) {
            if (row.getId() != null) {
                existingById.put(row.getId(), row);
            }
        }

        Set<Long> incomingIds = new HashSet<>();

        for (Map<String, Object> item : attributes) {
            Long ekipId = readLong(item, "id");
            ProductionEkip target;
            if (ekipId != null) {
                if (!incomingIds.add(ekipId)) {
                    throw new IllegalArgumentException("Duplicate production ekip id in request: " + ekipId);
                }
                target = existingById.get(ekipId);
                if (target == null) {
                    throw new IllegalArgumentException("Production ekip does not belong to movie profile " + movieProfileId + ": " + ekipId);
                }
            } else {
                target = new ProductionEkip();
            }

            target.setEkipName(readString(item, "ekipName"));
            target.setRole(parseEnum(ProductionRole.class, item.get("role")));
            target.setMovieProfile(managedProfile);
            if (ekipId == null) {
                managedProfile.getEkipMembers().add(target);
            }
        }

        managedProfile
            .getEkipMembers()
            .removeIf(row -> row.getId() != null && !incomingIds.contains(row.getId()));

        for (ProductionEkip row : managedProfile.getEkipMembers()) {
            row.setMovieProfile(managedProfile);
        }

        movieProfileRepository.save(managedProfile);
    }

    private List<ProductionEkip> loadProductionEkipsForProfile(Long movieProfileId) {
        List<ProductionEkip> matches = new ArrayList<>();
        int pageNumber = 0;
        while (true) {
            PageRequest request = PageRequest.of(pageNumber, BATCH_SIZE, Sort.by(Sort.Direction.ASC, "id"));
            Page<ProductionEkip> page = secureDataManager.loadByQuery(
                PRODUCTION_EKIP_CLASS,
                SecuredLoadQuery.of(EKIP_ENTITY_CODE, "production-ekip-list", request)
            );
            for (ProductionEkip ekip : page.getContent()) {
                Long mpId = ekip.getMovieProfile() != null ? ekip.getMovieProfile().getId() : null;
                if (movieProfileId.equals(mpId)) {
                    matches.add(ekip);
                }
            }
            if (!page.hasNext()) {
                break;
            }
            pageNumber++;
        }
        return matches;
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

    private Map<String, Object> toProductionEkipResponse(ProductionEkip source, Long movieProfileId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", source.getId());
        result.put("movieProfileId", movieProfileId);
        result.put("ekipName", source.getEkipName());
        result.put("role", source.getRole() != null ? source.getRole().name() : null);
        return result;
    }

    private String readString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : null;
    }
}
