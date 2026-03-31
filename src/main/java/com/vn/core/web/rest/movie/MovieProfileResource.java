package com.vn.core.web.rest.movie;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vn.core.domain.movie.MovieProfile;
import com.vn.core.domain.movie.enumeration.ekip.ProductionRole;
import com.vn.core.security.data.SecureDataManager.EntityMutation;
import com.vn.core.security.web.SecuredEntityJsonAdapter;
import com.vn.core.service.movie.MovieProfileService;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

@RestController
@RequestMapping("/api/movie-profiles")
@PreAuthorize("isAuthenticated()")
public class MovieProfileResource {

    private static final Logger LOG = LoggerFactory.getLogger(MovieProfileResource.class);
    private static final String LIST_FETCH_PLAN = "movie-profile-list";
    private static final String DETAIL_FETCH_PLAN = "movie-profile-detail";

    private final MovieProfileService movieProfileService;
    private final SecuredEntityJsonAdapter securedEntityJsonAdapter;
    private final ObjectMapper objectMapper;

    public MovieProfileResource(
        MovieProfileService movieProfileService,
        SecuredEntityJsonAdapter securedEntityJsonAdapter,
        ObjectMapper objectMapper
    ) {
        this.movieProfileService = movieProfileService;
        this.securedEntityJsonAdapter = securedEntityJsonAdapter;
        this.objectMapper = objectMapper;
    }

    @GetMapping("")
    @Transactional(readOnly = true)
    public ResponseEntity<String> getAllMovieProfiles(@ParameterObject Pageable pageable) {
        LOG.debug("REST request to get movie profiles");
        Page<MovieProfile> page = movieProfileService.list(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok()
            .headers(headers)
            .contentType(MediaType.APPLICATION_JSON)
            .body(securedEntityJsonAdapter.toJsonArrayString(page.getContent(), LIST_FETCH_PLAN));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<String> getMovieProfile(@PathVariable("id") Long id) {
        LOG.debug("REST request to get movie profile : {}", id);
        return movieProfileService
            .findOne(id)
            .map(profile ->
                ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(securedEntityJsonAdapter.toJsonString(profile, DETAIL_FETCH_PLAN))
            )
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("")
    @Transactional
    public ResponseEntity<String> createMovieProfile(@RequestBody String body) {
        LOG.debug("REST request to create movie profile");
        ObjectNode root = parseObjectBody(body);
        EkipMembersPayload ekipMembersPayload = extractEkipMembers(root);
        if (ekipMembersPayload.present()) {
            root.remove("ekipMembers");
        }
        EntityMutation<MovieProfile> mutation = securedEntityJsonAdapter.fromJson(root, MovieProfile.class);
        MovieProfile result = movieProfileService.create(mutation, ekipMembersPayload.members());
        return ResponseEntity.created(URI.create("/api/movie-profiles/" + result.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .body(securedEntityJsonAdapter.toJsonString(result, DETAIL_FETCH_PLAN));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<String> updateMovieProfile(@PathVariable("id") Long id, @RequestBody String body) {
        LOG.debug("REST request to update movie profile : {}", id);
        ObjectNode root = parseObjectBody(body);
        EkipMembersPayload ekipMembersPayload = extractEkipMembers(root);
        if (ekipMembersPayload.present()) {
            root.remove("ekipMembers");
        }
        EntityMutation<MovieProfile> mutation = securedEntityJsonAdapter.fromJson(root, MovieProfile.class);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                securedEntityJsonAdapter.toJsonString(
                    movieProfileService.update(id, mutation, ekipMembersPayload.members()),
                    DETAIL_FETCH_PLAN
                )
            );
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteMovieProfile(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete movie profile : {}", id);
        movieProfileService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    @Transactional(readOnly = true)
    public ResponseEntity<Long> countMovieProfiles() {
        LOG.debug("REST request to count movie profiles");
        return ResponseEntity.ok(movieProfileService.count());
    }

    @GetMapping("/next-code")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, String>> getNextProfileCode(@RequestParam("year") Integer year) {
        LOG.debug("REST request to next profile code for year : {}", year);
        return ResponseEntity.ok(Map.of("code", movieProfileService.nextCode(year)));
    }

    @GetMapping("/{id}/production-ekips")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getProductionEkips(@PathVariable("id") Long id) {
        LOG.debug("REST request to get production ekips for movie profile : {}", id);
        return ResponseEntity.ok(movieProfileService.listProductionEkips(id));
    }

    @PutMapping("/{id}/production-ekips")
    @Transactional
    public ResponseEntity<List<Map<String, Object>>> replaceProductionEkips(
        @PathVariable("id") Long id,
        @RequestBody List<Map<String, Object>> attributes
    ) {
        LOG.debug("REST request to replace production ekips for movie profile : {}", id);
        return ResponseEntity.ok(movieProfileService.replaceProductionEkips(id, attributes));
    }

    private ObjectNode parseObjectBody(String body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            if (!node.isObject()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body must be a JSON object");
            }
            return (ObjectNode) node;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body must be valid JSON", ex);
        }
    }

    private EkipMembersPayload extractEkipMembers(ObjectNode root) {
        JsonNode raw = root.get("ekipMembers");
        if (raw == null || raw.isNull()) {
            return EkipMembersPayload.absent();
        }
        if (!raw.isArray()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'ekipMembers' must be a JSON array");
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (JsonNode item : raw) {
            validateEkipMemberItem(item);
            result.add(objectMapper.convertValue(item, new TypeReference<Map<String, Object>>() {}));
        }
        return EkipMembersPayload.present(result);
    }

    private void validateEkipMemberItem(JsonNode item) {
        if (!item.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each 'ekipMembers' item must be a JSON object");
        }
        if (item.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'ekipMembers' items must not be empty objects");
        }

        JsonNode idNode = item.get("id");
        if (idNode != null && !idNode.isNull()) {
            if (idNode.isNumber()) {
                if (idNode.longValue() <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'ekipMembers[].id' must be greater than 0");
                }
            } else if (idNode.isTextual()) {
                try {
                    if (Long.parseLong(idNode.asText().trim()) <= 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'ekipMembers[].id' must be greater than 0");
                    }
                } catch (NumberFormatException ex) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'ekipMembers[].id' must be a valid number");
                }
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'ekipMembers[].id' must be a valid number");
            }
        }

        String ekipName = readRequiredText(item, "ekipName");
        if (ekipName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'ekipMembers[].ekipName' must not be blank");
        }

        String roleValue = readRequiredText(item, "role");
        try {
            ProductionRole.valueOf(roleValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'ekipMembers[].role' is invalid");
        }
    }

    private String readRequiredText(JsonNode item, String fieldName) {
        JsonNode value = item.get(fieldName);
        if (value == null || value.isNull() || !value.isTextual()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'ekipMembers[]." + fieldName + "' is required");
        }
        return value.asText().trim();
    }

    private record EkipMembersPayload(boolean present, List<Map<String, Object>> members) {
        private static EkipMembersPayload absent() {
            return new EkipMembersPayload(false, null);
        }

        private static EkipMembersPayload present(List<Map<String, Object>> members) {
            return new EkipMembersPayload(true, List.copyOf(members));
        }
    }
}
