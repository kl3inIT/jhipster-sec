package com.vn.core.web.rest.movie;

import com.vn.core.domain.movie.TopicOrientation;
import com.vn.core.security.data.SecureDataManager.EntityMutation;
import com.vn.core.security.web.SecuredEntityJsonAdapter;
import com.vn.core.service.movie.TopicOrientationService;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

@RestController
@RequestMapping("/api/topic-orientations")
@PreAuthorize("isAuthenticated()")
public class TopicOrientationResource {

    private static final Logger LOG = LoggerFactory.getLogger(TopicOrientationResource.class);
    private static final String LIST_FETCH_PLAN = "topic-orientation-list";
    private static final String DETAIL_FETCH_PLAN = "topic-orientation-detail";

    private final TopicOrientationService topicOrientationService;
    private final SecuredEntityJsonAdapter securedEntityJsonAdapter;

    public TopicOrientationResource(TopicOrientationService topicOrientationService, SecuredEntityJsonAdapter securedEntityJsonAdapter) {
        this.topicOrientationService = topicOrientationService;
        this.securedEntityJsonAdapter = securedEntityJsonAdapter;
    }

    @GetMapping("")
    @Transactional(readOnly = true)
    public ResponseEntity<String> getAllTopicOrientations(@ParameterObject Pageable pageable) {
        LOG.debug("REST request to get topic orientations");
        Page<TopicOrientation> page = topicOrientationService.list(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok()
            .headers(headers)
            .contentType(MediaType.APPLICATION_JSON)
            .body(securedEntityJsonAdapter.toJsonArrayString(page.getContent(), LIST_FETCH_PLAN));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<String> getTopicOrientation(@PathVariable("id") Long id) {
        LOG.debug("REST request to get topic orientation : {}", id);
        return topicOrientationService
            .findOne(id)
            .map(topicOrientation ->
                ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(securedEntityJsonAdapter.toJsonString(topicOrientation, DETAIL_FETCH_PLAN))
            )
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("")
    @Transactional
    public ResponseEntity<String> createTopicOrientation(@RequestBody String attributesJson) {
        LOG.debug("REST request to create topic orientation");
        EntityMutation<TopicOrientation> mutation = securedEntityJsonAdapter.fromJson(attributesJson, TopicOrientation.class);
        TopicOrientation result = topicOrientationService.create(mutation);
        return ResponseEntity.created(URI.create("/api/topic-orientations/" + result.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .body(securedEntityJsonAdapter.toJsonString(result, DETAIL_FETCH_PLAN));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<String> updateTopicOrientation(@PathVariable("id") Long id, @RequestBody String attributesJson) {
        LOG.debug("REST request to update topic orientation : {}", id);
        EntityMutation<TopicOrientation> mutation = securedEntityJsonAdapter.fromJson(attributesJson, TopicOrientation.class);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(securedEntityJsonAdapter.toJsonString(topicOrientationService.update(id, mutation), DETAIL_FETCH_PLAN));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteTopicOrientation(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete topic orientation : {}", id);
        topicOrientationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
