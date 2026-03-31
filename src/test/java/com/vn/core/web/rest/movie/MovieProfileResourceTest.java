package com.vn.core.web.rest.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.core.domain.movie.MovieProfile;
import com.vn.core.security.data.SecureDataManager.EntityMutation;
import com.vn.core.security.web.SecuredEntityJsonAdapter;
import com.vn.core.service.movie.MovieProfileService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MovieProfileResourceTest {

    @Mock
    private MovieProfileService movieProfileService;

    @Mock
    private SecuredEntityJsonAdapter securedEntityJsonAdapter;

    private MovieProfileResource resource;

    private EntityMutation<MovieProfile> mutation;
    private MovieProfile profile;

    @BeforeEach
    void setUp() {
        resource = new MovieProfileResource(movieProfileService, securedEntityJsonAdapter, new ObjectMapper());
        mutation = new EntityMutation<>(new MovieProfile(), Set.of("movieName"));
        profile = new MovieProfile();
        profile.setId(1101L);

        when(securedEntityJsonAdapter.fromJson(any(com.fasterxml.jackson.databind.JsonNode.class), eq(MovieProfile.class))).thenReturn(mutation);
        when(securedEntityJsonAdapter.toJsonString(profile, "movie-profile-detail")).thenReturn("{}");
    }

    @Test
    void updateWithoutEkipMembers_keepsExistingCollection() {
        when(movieProfileService.update(1101L, mutation, null)).thenReturn(profile);

        var response = resource.updateMovieProfile(1101L, """
            {
              "movieName": "sửa",
              "productionYear": "null"
            }
            """);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(movieProfileService).update(1101L, mutation, null);
    }

    @Test
    void updateWithEmptyEkipMembers_replacesCollectionWithEmptyList() {
        when(movieProfileService.update(1101L, mutation, List.of())).thenReturn(profile);

        var response = resource.updateMovieProfile(1101L, """
            {
              "movieName": "sửa",
              "ekipMembers": []
            }
            """);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(movieProfileService).update(1101L, mutation, List.of());
    }

    @Test
    void createWithoutEkipMembers_doesNotForceCollectionReplacement() {
        when(movieProfileService.create(mutation, null)).thenReturn(profile);

        var response = resource.createMovieProfile("""
            {
              "movieName": "mới"
            }
            """);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(movieProfileService).create(mutation, null);
    }

    @Test
    void updateRejectsNonArrayEkipMembers() {
        assertThatThrownBy(() ->
            resource.updateMovieProfile(1101L, """
                {
                  "movieName": "sửa",
                  "ekipMembers": {}
                }
                """)
        )
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
    @Test
    void updateRejectsEmptyEkipMemberObject() {
        assertThatThrownBy(() ->
            resource.updateMovieProfile(1101L, """
                {
                  "movieName": "update",
                  "ekipMembers": [
                    {}
                  ]
                }
                """)
        )
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void updateRejectsIncompleteEkipMemberObject() {
        assertThatThrownBy(() ->
            resource.updateMovieProfile(1101L, """
                {
                  "movieName": "update",
                  "ekipMembers": [
                    {
                      "ekipName": "Director"
                    }
                  ]
                }
                """)
        )
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void updateAcceptsValidEkipMembers() {
        when(movieProfileService.update(eq(1101L), eq(mutation), any(List.class))).thenReturn(profile);

        var response = resource.updateMovieProfile(1101L, """
            {
              "movieName": "update",
              "ekipMembers": [
                {
                  "id": 1709,
                  "ekipName": "Director",
                  "role": "DIRECTOR"
                }
              ]
            }
            """);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
