package com.vn.core.web.rest.movie;

import com.vn.core.service.movie.MovieProfileService;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

@RestController
@RequestMapping("/api/movie-profiles")
@PreAuthorize("isAuthenticated()")
public class MovieProfileResource {

    private static final Logger LOG = LoggerFactory.getLogger(MovieProfileResource.class);

    private final MovieProfileService movieProfileService;

    public MovieProfileResource(MovieProfileService movieProfileService) {
        this.movieProfileService = movieProfileService;
    }

    @GetMapping("")
    public ResponseEntity<List<Map<String, Object>>> getAllMovieProfiles(@ParameterObject Pageable pageable) {
        LOG.debug("REST request to get movie profiles");
        Page<Map<String, Object>> page = movieProfileService.list(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getMovieProfile(@PathVariable("id") Long id) {
        LOG.debug("REST request to get movie profile : {}", id);
        return ResponseUtil.wrapOrNotFound(movieProfileService.findOne(id));
    }

    @PostMapping("")
    public ResponseEntity<Map<String, Object>> createMovieProfile(@RequestBody Map<String, Object> attributes) {
        LOG.debug("REST request to create movie profile : {}", attributes);
        Map<String, Object> result = movieProfileService.create(attributes);
        return ResponseEntity.created(URI.create("/api/movie-profiles/" + result.get("id"))).body(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateMovieProfile(@PathVariable("id") Long id, @RequestBody Map<String, Object> attributes) {
        LOG.debug("REST request to update movie profile : {}", id);
        return ResponseEntity.ok(movieProfileService.update(id, attributes));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovieProfile(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete movie profile : {}", id);
        movieProfileService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countMovieProfiles() {
        LOG.debug("REST request to count movie profiles");
        return ResponseEntity.ok(movieProfileService.count());
    }

    @GetMapping("/next-code")
    public ResponseEntity<Map<String, String>> getNextProfileCode(@RequestParam("year") Integer year) {
        LOG.debug("REST request to next profile code for year : {}", year);
        return ResponseEntity.ok(Map.of("code", movieProfileService.nextCode(year)));
    }

    @GetMapping("/{id}/production-ekips")
    public ResponseEntity<List<Map<String, Object>>> getProductionEkips(@PathVariable("id") Long id) {
        LOG.debug("REST request to get production ekips for movie profile : {}", id);
        return ResponseEntity.ok(movieProfileService.listProductionEkips(id));
    }

    @PutMapping("/{id}/production-ekips")
    public ResponseEntity<List<Map<String, Object>>> replaceProductionEkips(
        @PathVariable("id") Long id,
        @RequestBody List<Map<String, Object>> attributes
    ) {
        LOG.debug("REST request to replace production ekips for movie profile : {}", id);
        return ResponseEntity.ok(movieProfileService.replaceProductionEkips(id, attributes));
    }
}
