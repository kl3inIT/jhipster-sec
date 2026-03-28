package com.vn.core.security.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MenuAppNameTest {

    @Test
    void fromValue_acceptsLegacyAndEnumRepresentations() {
        assertThat(MenuAppName.fromValue("movie app")).contains(MenuAppName.MOVIE_APP);
        assertThat(MenuAppName.fromValue("MOVIE_APP")).contains(MenuAppName.MOVIE_APP);
        assertThat(MenuAppName.MOVIE_APP.getValue()).isEqualTo("movie app");
    }
}
